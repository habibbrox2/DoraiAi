---
name: compose-resources
description: Fix missing Compose Resources strings and localization issues in DORAI AI without editing generated output or damaging translations.
---

# Compose Resources

1. Inspect `composeApp/src/commonMain/composeResources/values/strings.xml` and matching localized files.
2. Search the key's declarations and usages; distinguish Compose Resources from Android `res/values`.
3. Follow nearby generated API usage, usually `Res.string.*` with `stringResource`.
4. Preserve placeholders, translation meaning and locale-specific files; do not replace translations wholesale.
5. Never edit generated build output or guess generated package paths.
6. Validate XML and compile the target that consumes the resource.
7. Request approval before adding/removing resource keys or changing user-facing wording outside the explicit task.
