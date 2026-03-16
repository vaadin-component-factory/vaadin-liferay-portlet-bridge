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

import com.vaadin.flow.component.HasValue;

/**
 * A high-level Vaadin Portlet {@link RuntimeException}.
 */
public class VaadinPortletException extends RuntimeException {

    /**
     * Constructs a new Vaadin Portlet exception with the specified detail message.
     *
     * @param message
     *            the detail message
     */
    public VaadinPortletException(String message) {
        super(message);
    }


    /**
     * Constructs a new Vaadin Portlet exception with the specified detail
     * message and cause.
     *
     * @param message
     *            the detail message
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <code>null</code> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public VaadinPortletException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new Vaadin Portlet exception with the specified cause and a
     * detail message of <code>(cause==null ? null : cause.toString())</code>
     * (which typically contains the class and detail message of
     * <code>cause</code>).
     *
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <code>null</code> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public VaadinPortletException(Throwable cause) {
        super(cause);
    }
}
