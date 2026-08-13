/**
 * Copyright (C) 2019-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.portal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.portlet.PortletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

public class PortletWebComponentBootstrapHandlerTest {

    private PortletWebComponentBootstrapHandler handler = new PortletWebComponentBootstrapHandler();

    private DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);

    private VaadinPortletService service;

    @BeforeEach
    public void setUp() {
        service = Mockito.mock(VaadinPortletService.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        VaadinService.setCurrent(service);
    }

    @AfterEach
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void modifyPath_staticResourcesPathIsEmpty_pathIsPrefixedWithSlash() throws UnsupportedEncodingException {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        Mockito.when(configuration.getStringProperty(Mockito.eq(
                PortletConstants.PORTLET_PARAMETER_STATIC_RESOURCES_MAPPING),
                Mockito.anyString())).thenReturn("");
        String path = handler.modifyPath("bar", "./VAADIN/foo");
        Assertions.assertEquals("/./VAADIN/foo", path);
    }

    @Test
    public void modifyPath_staticResourcesPathHasNoSlashes_pathIsPrefixedAndPostfixedWithSlash() throws UnsupportedEncodingException {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        Mockito.when(configuration.getStringProperty(Mockito.eq(
                PortletConstants.PORTLET_PARAMETER_STATIC_RESOURCES_MAPPING),
                Mockito.anyString())).thenReturn("baz");
        String path = handler.modifyPath("bar", "./VAADIN/foo");
        Assertions.assertEquals("/baz/./VAADIN/foo", path);
    }

    @Test
    public void modifyPath_staticResourcesPathHasAllSlashes_pathIsConcatenatedWithMappingURI() throws UnsupportedEncodingException {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        Mockito.when(configuration.getStringProperty(Mockito.eq(
                PortletConstants.PORTLET_PARAMETER_STATIC_RESOURCES_MAPPING),
                Mockito.anyString())).thenReturn("/baz/");
        String path = handler.modifyPath("bar", "./VAADIN/foo");
        Assertions.assertEquals("/baz/./VAADIN/foo", path);
    }

    @Test
    public void writeBootstrapPage_windowNameIsAssignedBeforeAnyOtherScriptRuns()
            throws IOException {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getContext()).thenReturn(new MapContext());

        String namespace = "_com_example_MyPortlet_INSTANCE_abcd_";
        PortletResponse portletResponse = Mockito.mock(PortletResponse.class);
        Mockito.when(portletResponse.getNamespace()).thenReturn(namespace);

        VaadinPortletResponse response = Mockito
                .mock(VaadinPortletResponse.class);
        Mockito.when(response.getPortletResponse()).thenReturn(portletResponse);
        Mockito.when(response.getService()).thenReturn(service);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);

        UIInternals internals = Mockito.mock(UIInternals.class);
        Mockito.when(internals.getAppId()).thenReturn("wc-1");
        UI ui = Mockito.mock(UI.class);
        Mockito.when(ui.getInternals()).thenReturn(internals);
        UI.setCurrent(ui);

        Document document = Jsoup.parse("<html><head></head><body></body></html>");
        Element head = document.head();
        // Stands in for the script Flow prepends, which starts the client
        // engine and eventually asks the browser for its window name.
        head.appendElement("script")
                .appendText("window.Vaadin.Flow.initApplication('wc-1', {});");

        handler.writeBootstrapPage("text/javascript", response, head, "/svc/");

        String written = out.toString(StandardCharsets.UTF_8);
        int windowName = written.indexOf("window.name");
        int flowBootstrap = written.indexOf("initApplication");
        int appId = written.indexOf("appId");

        Assertions.assertTrue(written.contains("if (!window.name)"),
                "An existing window name must not be overwritten");
        Assertions.assertTrue(flowBootstrap > -1 && appId > -1,
                "Expected the Flow bootstrap and appId scripts to be written");
        Assertions.assertTrue(windowName < flowBootstrap,
                "The window name must be assigned before the Flow bootstrap "
                        + "script runs, otherwise tabs cannot be told apart");
        Assertions.assertTrue(windowName < appId,
                "The window name script must come first in the head");
    }

    private static class MapContext implements VaadinContext {

        private final Map<Class<?>, Object> attributes = new HashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getAttribute(Class<T> type,
                Supplier<T> defaultValueSupplier) {
            T value = (T) attributes.get(type);
            if (value == null && defaultValueSupplier != null) {
                value = defaultValueSupplier.get();
                attributes.put(type, value);
            }
            return value;
        }

        @Override
        public <T> void setAttribute(Class<T> clazz, T value) {
            attributes.put(clazz, value);
        }

        @Override
        public void removeAttribute(Class<?> clazz) {
            attributes.remove(clazz);
        }

        @Override
        public Enumeration<String> getContextParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getContextParameter(String name) {
            return null;
        }
    }
}
