/**
 * Copyright (C) 2019-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.portal;

import jakarta.portlet.ActionParameters;
import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;
import jakarta.portlet.ActionURL;
import jakarta.portlet.PortalContext;
import jakarta.portlet.PortletException;
import jakarta.portlet.PortletMode;
import jakarta.portlet.PortletRequest;
import jakarta.portlet.RenderRequest;
import jakarta.portlet.RenderResponse;
import jakarta.portlet.WindowState;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.portal.VaadinPortlet.PortletWebComponentExporter;
import com.vaadin.flow.portal.lifecycle.PortletEvent;
import com.vaadin.flow.portal.lifecycle.PortletModeEvent;
import com.vaadin.flow.portal.lifecycle.WindowStateEvent;
import com.vaadin.flow.server.SessionExpiredException;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

@NotThreadSafe
public class VaadinPortletTest {

    private class TestVaadinPortlet extends VaadinPortlet<TestComponent> {

        private static class TestWebComponentExporter
                extends PortletWebComponentExporter<TestComponent> {

            private TestWebComponentExporter(String tag) {
                super(tag);
            }
        }

        private TestWebComponentExporter exporter;

        @Override
        protected String getTitle(RenderRequest request) {
            return "";
        }

        @Override
        protected VaadinPortletService getService() {
            return service;
        }

        @Override
        public WebComponentExporter<TestComponent> create() {
            exporter = new TestWebComponentExporter(getPortletTag());
            return exporter;
        }
    };

    private static class TestComponent extends Div implements PortletView {

        private PortletViewContext context;

        private int initCounts;

        @Override
        public void onPortletViewContextInit(PortletViewContext context) {
            this.context = context;
            initCounts++;
        }

    }

    private static class TestMYPortlet extends VaadinPortlet<Div> {

    }

    private static class Wrapper {

        private static class TestMYPortlet extends VaadinPortlet<Div> {

        }

    }

    private static class Special$Character extends VaadinPortlet<Div> {

    }

    @StyleSheet("./eager.css")
    @StyleSheet(value = "./lazy.css", loadMode = LoadMode.LAZY)
    private static class StyledExporter
            extends PortletWebComponentExporter<Div> {
        StyledExporter(String tag) {
            super(tag);
        }
    }

    private String namespace = "namespace-foo";

    private TestVaadinPortlet portlet;
    private TestComponent component;
    private VaadinPortletService service;
    private VaadinPortletSession session;
    private UI ui;

    @BeforeEach
    public void setUp() throws SessionExpiredException {
        portlet = new TestVaadinPortlet();
        service = Mockito.mock(VaadinPortletService.class);

        session = new VaadinPortletSession(service) {
            @Override
            public boolean hasLock() {
                return true;
            }

            @Override
            public void lock() {
            }

            @Override
            public void unlock() {
            }
        };

        Mockito.when(service.getPortlet()).thenReturn(portlet);
        Mockito.when(service.findVaadinSession(Mockito.any()))
                .thenReturn(session);

        VaadinPortletService.setCurrent(service);

        VaadinPortletResponse response = Mockito
                .mock(VaadinPortletResponse.class);
        CurrentInstance.set(VaadinResponse.class, response);
        RenderResponse portletResponse = Mockito.mock(RenderResponse.class);
        Mockito.when(response.getPortletResponse()).thenReturn(portletResponse);

        Mockito.when(portletResponse.getNamespace()).thenReturn(namespace);

        VaadinPortletRequest request = Mockito.mock(VaadinPortletRequest.class);

        RenderRequest portletRequest = Mockito.mock(RenderRequest.class);
        Mockito.when(portletRequest.getPortletMode())
                .thenReturn(PortletMode.VIEW);
        Mockito.when(portletRequest.getWindowState())
                .thenReturn(WindowState.NORMAL);
        Mockito.when(request.getPortletRequest()).thenReturn(portletRequest);
        CurrentInstance.set(VaadinRequest.class, request);

        VaadinSession.setCurrent(session);

        ui = new UI() {
            @Override
            public VaadinSession getSession() {
                return session;
            }
        };
        UI.setCurrent(ui);

        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("mywindow");
        ui.getInternals().setExtendedClientDetails(details);

        Mockito.when(request.getPortletMode()).thenReturn(PortletMode.VIEW);
        Mockito.when(request.getWindowState()).thenReturn(WindowState.NORMAL);

        portlet.create();
        component = new TestComponent();
        ui.add(component);
        portlet.exporter.configureInstance(null, component);
    }

    @AfterEach
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void createExporter_getComponentClass_componentClassIsDetected() {
        Assertions.assertEquals(TestComponent.class,
                portlet.exporter.getComponentClass());
    }

    @Test
    public void configureInstance_styleSheetAnnotationOnExporter_dependenciesRegistered() {
        StyledExporter exporter = new StyledExporter("styled-portlet");
        Div div = new Div();
        ui.add(div);

        exporter.configureInstance(null, div);

        Dependency eager = ui.getInternals().getDependencyList()
                .getDependencyByUrl("./eager.css", Dependency.Type.STYLESHEET);
        Assertions.assertNotNull(eager,
                "@StyleSheet on exporter should register eager dependency");
        Assertions.assertEquals(LoadMode.EAGER, eager.getLoadMode());

        Dependency lazy = ui.getInternals().getDependencyList()
                .getDependencyByUrl("./lazy.css", Dependency.Type.STYLESHEET);
        Assertions.assertNotNull(lazy,
                "@StyleSheet on exporter should register lazy dependency");
        Assertions.assertEquals(LoadMode.LAZY, lazy.getLoadMode());
    }

    @Test
    public void configureInstance_reattachToNewUI_styleSheetsRegisteredOnNewUI() {
        // Simulates @PreserveOnRefresh: configureInstance runs once on the
        // original UI; on refresh the cached element is reattached to a fresh
        // WebComponentUI and configureInstance is bypassed. The attach listener
        // is what carries the stylesheet registration to the new UI.
        StyledExporter exporter = new StyledExporter("styled-portlet");
        Div div = new Div();
        ui.add(div);
        exporter.configureInstance(null, div);

        div.getElement().removeFromTree(false);
        UI refreshedUi = new UI() {
            @Override
            public VaadinSession getSession() {
                return session;
            }
        };
        UI.setCurrent(refreshedUi);
        refreshedUi.add(div);

        Dependency eager = refreshedUi.getInternals().getDependencyList()
                .getDependencyByUrl("./eager.css", Dependency.Type.STYLESHEET);
        Assertions.assertNotNull(eager,
                "stylesheets should be re-registered on the refreshed UI");
        Dependency lazy = refreshedUi.getInternals().getDependencyList()
                .getDependencyByUrl("./lazy.css", Dependency.Type.STYLESHEET);
        Assertions.assertNotNull(lazy,
                "stylesheets should be re-registered on the refreshed UI");
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void createExporter_exporterIsNotExtended_componentClassIsDetected() {
        TestMYPortlet portlet = new TestMYPortlet();

        PortletWebComponentExporter exporter = (PortletWebComponentExporter) portlet
                .create();
        Assertions.assertEquals(Div.class, exporter.getComponentClass());
    }

    @Test
    public void addWindowStateListener_stateIsChanged_listenerIsCalled()
            throws PortletException, IOException {
        Assertions.assertNotNull(component.context);

        AtomicReference<WindowStateEvent> listener = new AtomicReference<>();
        component.context.addWindowStateChangeListener(
                event -> Assertions.assertNull(listener.getAndSet(event)));

        requestModeAndState("foo", "bar");

        Assertions.assertNotNull(listener.get());
        Assertions.assertEquals("bar", listener.get().getWindowState().toString());
    }

    @Test
    public void addWindowStateListener_unregister_listenerIsNotCalled()
            throws PortletException, IOException {
        AtomicReference<WindowStateEvent> listener = new AtomicReference<>();
        Registration registration = component.context
                .addWindowStateChangeListener(
                        event -> Assertions.assertNull(listener.getAndSet(event)));

        requestModeAndState("foo", "bar");

        registration.remove();

        listener.set(null);

        requestModeAndState("foo", "baz");

        Assertions.assertNull(listener.get());
    }

    @Test
    public void addPortletModeListener_modeIsChanged_listenerIsCalled()
            throws PortletException, IOException {
        Assertions.assertNotNull(component.context);

        AtomicReference<PortletModeEvent> listener = new AtomicReference<>();
        component.context.addPortletModeChangeListener(
                event -> Assertions.assertNull(listener.getAndSet(event)));

        requestModeAndState("foo", "bar");

        Assertions.assertNotNull(listener.get());
        Assertions.assertEquals("foo", listener.get().getPortletMode().toString());
    }

    @Test
    public void addPortletModeListener_unregister_listenerIsNotCalled()
            throws PortletException, IOException {
        Assertions.assertNotNull(component.context);

        AtomicReference<PortletModeEvent> listener = new AtomicReference<>();
        Registration registration = component.context
                .addPortletModeChangeListener(
                        event -> Assertions.assertNull(listener.getAndSet(event)));

        requestModeAndState("foo", "bar");

        registration.remove();

        listener.set(null);

        requestModeAndState("baz", "bar");

        Assertions.assertNull(listener.get());
    }

    @Test
    public void configure_onPortletViewContextInitIsCalledOnce_listenersAreNotCalledTwice()
            throws PortletException, IOException {
        Assertions.assertEquals(1, component.initCounts);

        AtomicReference<PortletModeEvent> listener = new AtomicReference<>();
        component.context.addPortletModeChangeListener(
                event -> Assertions.assertNull(listener.getAndSet(event)));

        // re-attach
        requestModeAndState("foo", "bar");

        Assertions.assertEquals(1, component.initCounts);

        listener.set(null);
        // fire an event one more time, listener should not throw ( should not
        // be called twice)
        requestModeAndState("foo", "bar");
    }

    private void requestModeAndState(String portletMode, String windowState) {
        RenderRequest request = Mockito.mock(RenderRequest.class);
        RenderResponse response = Mockito.mock(RenderResponse.class);

        Mockito.when(response.getNamespace()).thenReturn(namespace);

        ActionURL url = Mockito.mock(ActionURL.class);
        Mockito.when(url.toString()).thenReturn("");
        Mockito.when(response.createActionURL()).thenReturn(url);

        PortletMode mode = Mockito.mock(PortletMode.class);
        Mockito.when(mode.toString()).thenReturn(portletMode);
        Mockito.when(request.getPortletMode()).thenReturn(mode);

        WindowState state = Mockito.mock(WindowState.class);
        Mockito.when(state.toString()).thenReturn(windowState);
        Mockito.when(request.getWindowState()).thenReturn(state);

        PortletRequest portletRequest = Mockito.mock(PortletRequest.class);
        Mockito.when(portletRequest.getPortletMode()).thenReturn(mode);
        Mockito.when(portletRequest.getWindowState()).thenReturn(state);

        VaadinPortletRequest vaadinPortletRequest = Mockito.mock(VaadinPortletRequest.class);
        Mockito.when(vaadinPortletRequest.getPortletRequest()).thenReturn(portletRequest);
        CurrentInstance.set(VaadinRequest.class, vaadinPortletRequest);

        VaadinPortletResponse vaadinPortletResponse =
                Mockito.mock(VaadinPortletResponse.class);
        Mockito.when(vaadinPortletResponse.getPortletResponse()).thenReturn(response);
        CurrentInstance.set(VaadinResponse.class, vaadinPortletResponse);

        // detach
        ui.remove(component);
        // attach
        ui.add(component);
    }

    @Test
    public void doDispatch_devServerEnabled_showErrorMessage()
            throws PortletException, IOException, NoSuchFieldException,
            IllegalAccessException {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        VaadinPortletResponse response = (VaadinPortletResponse) CurrentInstance
                .get(VaadinResponse.class);
        RenderResponse renderResponse = Mockito.mock(RenderResponse.class);
        Mockito.when(response.getPortletResponse()).thenReturn(renderResponse);
        Mockito.when(renderResponse.getNamespace()).thenReturn(namespace);
        StringWriter stringWriter = new StringWriter();
        Mockito.when(renderResponse.getWriter())
                .thenReturn(new PrintWriter(stringWriter));

        VaadinPortletRequest request = (VaadinPortletRequest) CurrentInstance
                .get(VaadinRequest.class);
        RenderRequest renderRequest = Mockito.mock(RenderRequest.class);
        Mockito.when(request.getPortletRequest()).thenReturn(renderRequest);

        portlet.doDispatch(renderRequest, renderResponse);

        Field devModeErrorMessageField = VaadinPortlet.class
                .getDeclaredField("DEV_MODE_ERROR_MESSAGE");
        devModeErrorMessageField.setAccessible(true);
        String expectedDevModeErrorMessage = (String) devModeErrorMessageField
                .get(null);
        Assertions.assertEquals(expectedDevModeErrorMessage,
                stringWriter.toString().trim(),
                "When dev server is enabled, DEV_MODE_ERROR_MESSAGE should be shown in the portlet.");
    }

    @Test
    public void getTag_tagNameDoNoContainUpperCaseLetters() {
        TestMYPortlet portlet = new TestMYPortlet();
        String tag = portlet.getPortletTag();
        Assertions.assertFalse(tag.chars().anyMatch(Character::isUpperCase));
    }

    @Test
    public void getTag_sameSimpleClassNamesDoNotCollide() {
        TestMYPortlet portlet = new TestMYPortlet();
        String tag = portlet.getPortletTag();
        Assertions.assertNotEquals(tag,
                new Wrapper.TestMYPortlet().getPortletTag());
    }

    @Test
    public void getTag_tagNameDoNoContainUpperCaseLettersAndDollarSign() {
        Special$Character portlet = new Special$Character();
        String tag = portlet.getPortletTag();
        Assertions.assertFalse(tag.chars().anyMatch(Character::isUpperCase));
        Assertions.assertFalse(tag.chars().anyMatch(ch -> ch == '$'));
    }

    @Test
    public void processAction_eventIsFiredAndNoExceptions()
            throws PortletException, SessionExpiredException {
        VaadinSession.setCurrent(null);
        ReentrantLock lock = new ReentrantLock();
        VaadinPortletSession session = new VaadinPortletSession(service) {

            @Override
            public Object getAttribute(String name) {
                return super.getAttribute(name);
            }

            @Override
            public Lock getLockInstance() {
                return lock;
            };
        };

        Map<String, PortletViewContext> viewContexts = new HashMap<>();

        Div div = new Div();
        ui.add(div);
        PortletViewContext context = new PortletViewContext(div,
                new AtomicBoolean(true), PortletMode.UNDEFINED,
                WindowState.UNDEFINED);
        viewContexts.put(namespace, context);

        AtomicReference<PortletEvent> listener = new AtomicReference<>();
        context.addEventChangeListener("foo",
                event -> Assertions.assertNull(listener.getAndSet(event)));
        ui.getInternals().setSession(session);

        ActionParameters params = Mockito.mock(ActionParameters.class);

        session.accessSynchronously(() -> {
            session.setAttribute(
                    TestVaadinPortlet.class.getName() + "-bar-viewContext",
                    viewContexts);
            Mockito.when(params.getValue("vaadin.uid"))
                    .thenReturn(getListenerUid());
        });

        VaadinSession.setCurrent(session);
        Mockito.when(service.findVaadinSession(Mockito.any()))
                .thenReturn(session);

        ActionRequest request = Mockito.mock(ActionRequest.class);
        ActionResponse response = Mockito.mock(ActionResponse.class);
        PortalContext portalContext = Mockito.mock(PortalContext.class);

        Mockito.when(response.getNamespace()).thenReturn(namespace);

        Mockito.when(request.getActionParameters()).thenReturn(params);
        Mockito.when(request.getPortalContext()).thenReturn(portalContext);

        Mockito.when(portalContext.getPortalInfo()).thenReturn("");
        Mockito.when(params.getNames())
                .thenReturn(Collections.singleton("vaadin.ev"));

        Mockito.when(params.getValue("vaadin.ev")).thenReturn("foo");
        Mockito.when(params.getValue("vaadin.wn")).thenReturn("bar");
        portlet.processAction(request, response);

        Assertions.assertNotNull(listener.get());
    }

    @Test
    public void initComponent_noViewContextExists_viewContextIsAddedToSession() {
        VaadinPortlet.initComponent(component);
        String attributeName = TestVaadinPortlet.class.getName() + "-"
                + "mywindow-viewContext";
        Map<String, Object> map = (Map<String, Object>) session
                .getAttribute(attributeName);
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map.containsKey(namespace));
        PortletViewContext context = (PortletViewContext) map.get(namespace);
        Assertions.assertEquals(component.context, context);
    }

    private String getListenerUid() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        PendingJavaScriptInvocation invocation = ui.getInternals()
                .dumpPendingJavaScriptInvocations().get(0);
        String uid = invocation.getInvocation().getParameters().get(2)
                .toString();
        return uid;
    }
}
