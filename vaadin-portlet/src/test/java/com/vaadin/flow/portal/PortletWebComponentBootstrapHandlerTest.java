/**
 * Copyright (C) 2019-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.portal;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinService;

public class PortletWebComponentBootstrapHandlerTest {

    private PortletWebComponentBootstrapHandler handler = new PortletWebComponentBootstrapHandler();

    private DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);

    @BeforeEach
    public void setUp() {
        VaadinPortletService service = Mockito.mock(VaadinPortletService.class);
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
}
