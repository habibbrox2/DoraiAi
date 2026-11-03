# Technology Boundaries

| Concern                  | Kotlin                         | TypeScript                          |
| ------------------------ | ------------------------------ | ----------------------------------- |
| App lifecycle            | Authoritative                  | Not authoritative                   |
| Native/shared Compose UI | Authoritative                  | Not a replacement                   |
| Agent orchestration      | Authoritative                  | Requests only through approved APIs |
| Provider credentials     | Owns secure storage/use        | Never receives raw secrets          |
| Shell/filesystem/sandbox | Owns execution and permissions | No unrestricted access              |
| Browser UI/integrations  | Host capability provider       | Future implementation               |
| Cross-language contracts | Validates/consumes             | Validates/consumes                  |
| Existing Kotlin/Wasm     | Owns target/build              | Separate workspace                  |

## Rules

1. Do not duplicate business logic across languages without a documented reason.
2. Keep transport DTOs separate from domain models.
3. Never treat TypeScript validation as a security boundary.
4. Do not execute model-generated or remotely supplied code in privileged contexts.
5. Keep Gradle and pnpm dependency graphs independent.
6. Any future exception requires an Architecture Decision Record.
