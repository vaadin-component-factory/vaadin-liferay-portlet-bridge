/**
 * Copyright (C) 2019-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.portal;

import jakarta.portlet.PortletContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.InitParameters;

/**
 * Tests for VaadinServletContext attribute storage and property delegation.
 */
public class VaadinPortletContextTest {

    private static String testAttributeProvider() {
        return "RELAX_THIS_IS_A_TEST";
    }

    private VaadinPortletContext context;

    private final Map<String, Object> attributeMap = new HashMap<>();
    private Map<String, String> properties;

    @BeforeEach
    public void setup() {
        PortletContext portletContext = Mockito.mock(PortletContext.class);
        Mockito.when(portletContext.getAttribute(Mockito.anyString())).then(invocationOnMock -> attributeMap.get(invocationOnMock.getArguments()[0].toString()));
        Mockito.doAnswer(invocationOnMock -> attributeMap.put(
                invocationOnMock.getArguments()[0].toString(),
                invocationOnMock.getArguments()[1]
        )).when(portletContext).setAttribute(Mockito.anyString(), Mockito.any());

        properties = new HashMap<>();
        properties.put(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE, "true");
        // Note: SERVLET_PARAMETER_ENABLE_DEV_SERVER was removed in Vaadin 25
        properties.put(InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE, "false");

        Mockito.when(portletContext.getInitParameterNames())
                .thenReturn(Collections.enumeration(properties.keySet()));
        Mockito.when(portletContext.getInitParameter(Mockito.anyString()))
                .then(invocation -> properties
                        .get(invocation.getArguments()[0]));
        context = new VaadinPortletContext(portletContext);
    }

    @Test
    public void getAttributeWithProvider() {
        Assertions.assertNull(context.getAttribute(String.class));

        String value = context.getAttribute(String.class,
                VaadinPortletContextTest::testAttributeProvider);
        Assertions.assertEquals(testAttributeProvider(), value);

        Assertions.assertEquals(testAttributeProvider(),
                context.getAttribute(String.class),
                "Value from provider should be persisted");
    }

    @Test
    public void setNullAttributeNotAllowed() {
        Assertions.assertThrows(AssertionError.class,
                () -> context.setAttribute(null));
    }

    @Test
    public void getMissingAttributeWithoutProvider() {
        String value = context.getAttribute(String.class);
        Assertions.assertNull(value);
    }

    @Test
    public void setAndGetAttribute() {
        String value = testAttributeProvider();
        context.setAttribute(value);
        String result = context.getAttribute(String.class);
        Assertions.assertEquals(value, result);
        // overwrite
        String newValue = "this is a new value";
        context.setAttribute(newValue);
        result = context.getAttribute(String.class);
        Assertions.assertEquals(newValue, result);
        // now the provider should not be called, so value should be still there
        result = context.getAttribute(String.class,
                () -> {
                    throw new AssertionError("Should not be called");
                });
        Assertions.assertEquals(newValue, result);
    }

    @Test
    public void testGetPropertyNames_returnsExpectedProperties() {
        List<String> list = Collections.list(context.getContextParameterNames());
        Assertions.assertEquals(properties.size(), list.size(),
                "Context should return only keys defined in PortletContext");
        for (String key : properties.keySet()) {
            Assertions.assertEquals(properties.get(key),
                    context.getContextParameter(key),
                    String.format(
                            "Value should be same from context for key '%s'",
                            key));
        }
    }

}
