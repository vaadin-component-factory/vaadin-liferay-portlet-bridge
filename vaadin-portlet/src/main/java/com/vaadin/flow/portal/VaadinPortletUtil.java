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
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>} for the full
 * license.
 * #L%
 */

import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;

/**
 * Static utility helpers shared across the portlet bridge.
 * <p>
 * For internal use only.
 */
final class VaadinPortletUtil implements Serializable {

    private VaadinPortletUtil() {
    }

    /**
     * Normalizes a window name so that session-key lookups are consistent.
     * <p>
     * Java {@code null} and the literal string {@code "null"} (either of
     * which {@code ExtendedClientDetails.getWindowName()} may return) are
     * collapsed to an empty string. Any other value is returned as-is.
     *
     * @param windowName
     *            raw window name value
     * @return normalized window name, never {@code null}
     */
    static String normalizeWindowName(String windowName) {
        if (windowName == null || "null".equals(windowName)) {
            return "";
        }
        return windowName;
    }

    /**
     * Returns the normalized window name from the given UI's
     * {@link ExtendedClientDetails}, or {@code null} if the UI has no
     * resolved client details yet.
     * <p>
     * See {@link #normalizeWindowName(String)} for normalization semantics.
     *
     * @param ui
     *            the UI to inspect; {@code null} is tolerated and yields
     *            {@code null}
     * @return normalized window name, or {@code null} when no client details
     *         are available
     */
    static String normalizedWindowName(UI ui) {
        if (ui == null) {
            return null;
        }
        ExtendedClientDetails details = ui.getInternals()
                .getExtendedClientDetails();
        if (details == null) {
            return null;
        }
        return normalizeWindowName(details.getWindowName());
    }
}
