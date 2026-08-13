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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;

import java.io.Serializable;

/**
 * Static utility helpers shared across the portlet bridge.
 * <p>
 * For internal use only.
 */
public final class VaadinPortletUtil implements Serializable {

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
   public static String normalizeWindowName(String windowName) {
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
    public static String normalizedWindowName(UI ui) {
        if (ui == null) {
            return null;
        }
        final var details = ui.getInternals().getExtendedClientDetails();
        if (details == null) {
            return null;
        }
        return normalizeWindowName(details.getWindowName());
    }

    /**
     * Returns the raw, un-normalized window name reported by the given UI's
     * {@link ExtendedClientDetails}, or {@code null} when the UI has no client
     * details.
     *
     * @param ui
     *            the UI to inspect; {@code null} is tolerated and yields
     *            {@code null}
     * @return the window name as reported by the client, may be {@code null}
     */
    public static String rawWindowName(UI ui) {
        if (ui == null) {
            return null;
        }
        final var details = ui.getInternals().getExtendedClientDetails();
        if (details == null) {
            return null;
        }
        return details.getWindowName();
    }

    /**
     * Returns the window name to key session state on, falling back to
     * {@code fallback} when the client has not reported a usable one.
     * <p>
     * A blank window name must never become part of a key: it is the same for
     * every browser tab, so all tabs of a session would end up sharing the
     * state that is meant to be per tab. The client details may also be
     * missing entirely, e.g. before the browser has been asked for them.
     *
     * @param ui
     *            the UI to take the window name from
     * @param fallback
     *            value to use when no usable window name is available,
     *            typically the portlet namespace
     * @return a non-blank key part
     */
    public static String windowNameOrFallback(UI ui, String fallback) {
        final String windowName = normalizedWindowName(ui);
        if (windowName == null || windowName.isEmpty()) {
            return fallback;
        }
        return windowName;
    }
}
