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

import java.util.Collection;

import jakarta.portlet.ClientDataRequest;
import jakarta.portlet.PortletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.communication.StreamReceiverHandler;

/**
 * Extends {@link StreamReceiverHandler} to handle upload
 * {@link VaadinRequest}s where the underlying request is of type
 * {@link ClientDataRequest} instead of {@link HttpServletRequest}.
 * <p>
 * For internal use only.
 *
 * @author Vaadin Ltd
 * @since
 */
class PortletStreamReceiverHandler extends StreamReceiverHandler {

    @Override
    protected boolean isMultipartUpload(VaadinRequest request) {
        if (!(request instanceof VaadinPortletRequest)) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase().startsWith("multipart/");
    }

    @Override
    protected Collection<Part> getParts(VaadinRequest request)
            throws Exception {
        PortletRequest portletRequest =
                ((VaadinPortletRequest) request).getPortletRequest();
        return ((ClientDataRequest) portletRequest).getParts();
    }
}
