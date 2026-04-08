# Vaadin 25 Compatibility Assessment

## Context
The vaadin-portlet bridge is currently on Vaadin 24.9.13 / Flow 24.9.14. This document assesses what it would take to support Vaadin 25.

## Current State
- **Vaadin**: 24.9.13, **Flow**: 24.9.14
- **Java**: 17
- **Jakarta APIs**: Already using `jakarta.portlet`, `jakarta.servlet`
- Version properties in `pom.xml` (lines 81-82) and `vaadin-portlet/pom.xml` (lines 60-61)

## Breaking Changes That Affect This Bridge

### 1. Java 21+ Required (BLOCKER)
Vaadin 25 requires Java 21. The project currently targets Java 17. All POMs and CI/build configs need updating.

### 2. @PreserveOnRefresh Behavior Change (CRITICAL)
**This directly impacts our recent fixes.** In Vaadin 25:
- UI instances are **no longer preserved** between refreshes
- The view is detached from the previous UI and attached to a **fresh UI instance**
- Components receive `AttachEvent` and `DetachEvent` when moved to new UI instances

**Impact**: Our `updateViewContextFromRender` fix in `doDispatch` (commit `5584f4a`) was needed because `@PreserveOnRefresh` prevented `initComponent` from being re-called. In Vaadin 25, the attach listener WILL fire on each refresh (since the component is re-attached to a new UI), meaning:
- `initComponent` will be called again via the attach listener
- Our `context.getView() != component` check (commit `bca47b2`) will work correctly since it's the same component but re-attached
- The `updateViewContextFromRender` call in `doDispatch` may become redundant but won't cause harm
- The `activeContexts` map on VaadinPortlet may need adjustment since the portlet instance lifecycle could differ

### 3. Polymer to Lit Migration (MEDIUM)
Components migrated from Polymer to Lit. Our `Object.getOwnPropertyNames` patch in PortletMethods.js was specifically to work around Polymer's `PropertyAccessors` mixin assigning to `{}` objects. If Vaadin 25 uses LitElement instead of Polymer:
- The `toJsonURLText` error may no longer occur in Polymer code
- But could still occur in LitElement if it does similar prototype walks
- The patch is safe to keep -- it's a no-op when there are no non-writable/non-configurable properties on `Object.prototype`

### 4. JSON API Change -- Elemental JSON to Jackson 3 (LOW for this bridge)
The bridge doesn't directly use `JsonObject`/`JsonArray` from Elemental. Unlikely to be affected.

### 5. WebComponentExporter @CssImport Scoping (LOW)
Stylesheets now only apply to shadow DOM. The bridge doesn't use `@CssImport` -- portlet apps do. This is a concern for consumers, not the bridge itself.

### 6. Jakarta EE 11 / Servlet 6.1 (MEDIUM)
Vaadin 25 targets Jakarta EE 11. Need to verify that:
- Liferay DXP 2026.Q1 supports Jakarta EE 11
- The portlet API version (`jakarta.portlet`) is compatible
- No servlet API breaks affect `VaadinPortletRequest`/`VaadinPortletResponse`

### 7. WebComponentBootstrapHandler Internals (HIGH RISK)
`PortletWebComponentBootstrapHandler` extends `WebComponentBootstrapHandler` from Flow. This is an internal API that may change without notice between major versions. Need to verify:
- `writeBootstrapPage` method signature
- `modifyPath` method
- Bootstrap script generation

### 8. Build System -- No Separate Production Profile (LOW)
`flow-maven-plugin` configuration may need updating. Production builds are default in V25.

## Summary

| Change | Risk | Action Needed |
|--------|------|--------------|
| Java 21 | BLOCKER | Update POMs, CI, build configs |
| @PreserveOnRefresh | CRITICAL | Test thoroughly; our fixes may need adjustment |
| WebComponentBootstrapHandler | HIGH | Verify internal API compatibility |
| Jakarta EE 11 | MEDIUM | Verify Liferay DXP compatibility |
| Polymer to Lit | MEDIUM | Test; JS patch likely safe as-is |
| JSON API | LOW | Unlikely to affect bridge |
| @CssImport scoping | LOW | Consumer concern, not bridge |
| Build system | LOW | Update maven plugin config |

## Recommendation
It won't "just work" -- primarily due to the Java 21 requirement and potential internal API changes in `WebComponentBootstrapHandler`. The @PreserveOnRefresh behavior change is the most nuanced: our fixes will likely still work but the interaction is different and needs testing. A bump of version properties alone is insufficient; a proper migration and testing pass is needed.

## Sources
- https://vaadin.com/docs/latest/upgrading
- https://vaadin.com/blog/upgrading-your-add-on-to-vaadin-25-guide
- https://vaadin.com/blog/vaadin-25-0-release
- https://github.com/vaadin/platform/issues/7691
- https://vaadin.com/docs/latest/flow/advanced/preserving-state-on-refresh
