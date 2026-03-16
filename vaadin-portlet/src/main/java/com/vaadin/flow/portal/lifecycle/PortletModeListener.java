/**
 * Copyright (C) 2019-2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.portal.lifecycle;

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

import java.io.Serializable;

/**
 * A listener portlet mode change events.
 *
 * @see com.vaadin.flow.portal.PortletViewContext
 * @see com.vaadin.flow.portal.lifecycle.PortletModeHandler
 * @author Vaadin Ltd
 * @since
 */
public interface PortletModeListener extends Serializable {

    /**
     * Invoked when the portlet mode changes.
     *
     * @param event
     *            the event object
     */
    void portletModeChange(PortletModeEvent event);
}
