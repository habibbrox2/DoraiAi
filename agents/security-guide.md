# Security and Regression Guide

DORAI AI describes itself as a security-hardened fork. A successful compile is not permission to weaken security behavior.

## Security invariants to preserve
- MCP URL validation protects against SSRF, unsafe schemes/addresses/credentials/encoding, revalidates connections and redirects, and sanitizes headers.
- Shell execution is intended to fail closed: allowlist validation and direct process execution rather than interpreting an unrestricted shell command string.
- Android network policy is HTTPS-first, with cleartext restricted to documented local endpoints.
- Android backup is disabled and sensitive preferences/conversation database material is excluded from backup rules.
- Release artifacts use SHA-256 checksums.
- Secure settings and conversation storage protect credentials and sensitive data.

## Required review for security-sensitive changes
- Trace input from UI/model/tool arguments through validation to the actual network/process/storage operation.
- Verify deny-by-default behavior, error handling, redirects, encoded input, path traversal, shell metacharacters, and permission denial as relevant.
- Never log API keys, passwords, tokens, private prompts, conversation contents or sensitive file paths.
- Do not add permissive network config, broad storage permissions, backup exceptions or shell bypasses to fix a build.
- Add/adjust regression tests when behavior changes, but do not weaken existing tests to get a green result.

## Before finishing
Review the diff for accidental security policy changes, removed validation, exposed secrets, weakened tests, and unrelated formatting. Report security checks not run.
