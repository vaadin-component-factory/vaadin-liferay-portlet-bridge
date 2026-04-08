# Vaadin Portlet Bridge — Audit Findings

## Critical / High Priority

### 1. Script load failures are silent
**File:** `src/main/resources/META-INF/resources/scripts/LiferayPortletRegistrationHelper.js:20-28`

The `createScript` Promise never rejects on `onerror`. If a script fails to load, the poller runs indefinitely then silently gives up with a `console.log`.

### 2. Polling loops have no max retries
**Files:** `src/main/resources/META-INF/resources/scripts/PortletMethods.js` — `executeWhenHubIdle` (line 50), `waitForHub` (line 66), `eventPoller` (line 272)

All poll with `setTimeout(fn, 10)` infinitely. If the hub never becomes idle or never registers, these loop forever (memory leak).

### 3. Race condition in afterServerUpdate hook
**File:** `src/main/resources/META-INF/resources/scripts/PortletMethods.js:230-239`

The hook restores the original `afterServerUpdate` after the first call, so subsequent server updates skip hub registration.

### 4. Hub registration failures are invisible to application
**File:** `src/main/resources/META-INF/resources/scripts/PortletMethods.js:207-223`

`.catch()` only logs the error. No state is set for the application to detect and handle gracefully.

### 5. `activeContexts` HashMap is not thread-safe
**File:** `src/main/java/com/vaadin/flow/portal/VaadinPortlet.java:151`

`private final Map<String, PortletViewContext> activeContexts = new HashMap<>()` is read/written without synchronization. Should be `ConcurrentHashMap`.

## Medium Priority

### 6. Event listener leak on portlet removal
**File:** `src/main/resources/META-INF/resources/scripts/PortletMethods.js:210, 310`

`hub.addEventListener` calls are never cleaned up when portlet elements are removed from the page. Causes memory leaks in long-lived pages.

### 7. Session expiration is unhandled (TODO)
**File:** `src/main/java/com/vaadin/flow/portal/VaadinPortletService.java:259-262`

`handleSessionExpired` only logs a debug message. No user feedback or recovery.

### 8. Object.prototype patching may be unnecessary
**File:** `src/main/resources/META-INF/resources/scripts/PortletMethods.js:23-35`

Heavy-handed global monkey-patch that filters non-writable, non-configurable properties from `Object.getOwnPropertyNames(Object.prototype)`. Originally needed for Liferay's `toJsonURLText` property breaking Polymer/LitElement. May not be needed on DXP 2026.q1.

### 9. Hardcoded timeouts with no configuration
**File:** `src/main/resources/META-INF/resources/scripts/PortletMethods.js`

Scattered polling intervals (10ms, 50ms, 100ms) with no way to configure for slow networks or high-latency environments:
- Line 50: `executeWhenHubIdle` — 10ms
- Line 66: `waitForHub` in `setPortletState` — 10ms
- Line 88: `waitForHub` in `fireEvent` — 10ms
- Line 253: `afterServerUpdate` fallback — 100ms
- Line 272: `eventPoller` — 10ms

### 10. `cachedNamespace` could go stale
**File:** `src/main/java/com/vaadin/flow/portal/PortletViewContext.java:72, 488-501`

`cachedNamespace` is set from the current response and reused when no response is available. If the same `PortletViewContext` is reused across different responses, the cached value could be wrong.

## Low Priority

### 11. Outdated SLF4J version
**File:** `pom.xml:46`

SLF4J 1.7.25 (2018) — 1.7.x is in maintenance mode. Upgrade to 2.0.x.

### 12. Outdated Maven plugins
**File:** `pom.xml`

- `maven-surefire-plugin: 2.22.2` (2018) — latest 3.x
- `maven-failsafe-plugin: 2.22.2` (2018) — latest 3.x
- `maven-war-plugin: 3.1.0` (2017) — latest 3.4.x

### 13. No JavaScript test coverage
Neither `PortletMethods.js` nor `LiferayPortletRegistrationHelper.js` have any tests. Polling logic, error handling, and race conditions are untested.

### 14. Copy-paste code flagged
**File:** `src/main/java/com/vaadin/flow/portal/VaadinPortletResponse.java:148`

Comment: `// FIXME This is copy paste` in `setCacheTime()` method.
