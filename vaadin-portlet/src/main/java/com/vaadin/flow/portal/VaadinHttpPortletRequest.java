/**
 * Copyright (C) 2019-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.portal;

/*-
 * #%L
 * Vaadin Liferay Portlet Bridge
 * %%
 * Copyright (C) 2026 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 * 
 * See the file license.html distributed with this software for more
 * information about licensing.
 * 
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */

import jakarta.portlet.PortletRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;

/**
 * Base class for portlet requests that need access to HTTP servlet
 * requests.
 *
 * @since
 */
public abstract class VaadinHttpPortletRequest extends VaadinPortletRequest {

    /**
     * Constructs a new {@link VaadinHttpPortletRequest}.
     *
     * @param request
     *         {@link PortletRequest} to be wrapped
     * @param vaadinService
     *         {@link VaadinPortletService} associated with this request
     */
    public VaadinHttpPortletRequest(PortletRequest request,
            VaadinPortletService vaadinService) {
        super(request, vaadinService);
    }

    private HttpServletRequest originalRequest;

    /**
     * Returns the original HTTP servlet request for this portlet request.
     *
     * @param request
     *         {@link PortletRequest} used to
     * @return the original HTTP servlet request
     */
    protected abstract HttpServletRequest getServletRequest(
            PortletRequest request);

    private HttpServletRequest getOriginalRequest() {
        if (originalRequest == null) {
            PortletRequest request = getRequest();
            originalRequest = getServletRequest(request);
        }

        return originalRequest;
    }

    @Override
    public String getParameter(String name) {
        String parameter = super.getParameter(name);
        if (parameter == null && getOriginalRequest() != null) {
            parameter = getOriginalRequest().getParameter(name);
        }
        return parameter;
    }

    @Override
    public String getRemoteAddr() {
        if (getOriginalRequest() != null) {
            return getOriginalRequest().getRemoteAddr();
        } else {
            return super.getRemoteAddr();
        }

    }

    @Override
    public String getRemoteHost() {
        if (getOriginalRequest() != null) {
            return getOriginalRequest().getRemoteHost();
        } else {
            return super.getRemoteHost();
        }
    }

    @Override
    public int getRemotePort() {
        if (getOriginalRequest() != null) {
            return getOriginalRequest().getRemotePort();
        } else {
            return super.getRemotePort();
        }
    }

    @Override
    public String getHeader(String name) {
        String header = super.getHeader(name);
        if (header == null && getOriginalRequest() != null) {
            header = getOriginalRequest().getHeader(name);
        }
        return header;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Enumeration<String> headerNames = super.getHeaderNames();
        if (headerNames == null && getOriginalRequest() != null) {
            headerNames = getOriginalRequest().getHeaderNames();
        }
        return headerNames;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        Enumeration<String> headers = super.getHeaders(name);
        if (headers == null && getOriginalRequest() != null) {
            headers = getOriginalRequest().getHeaders(name);
        }
        return headers;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameterMap = super.getParameterMap();
        if (parameterMap == null && getOriginalRequest() != null) {
            parameterMap = getOriginalRequest().getParameterMap();
        }
        return parameterMap;
    }
}
