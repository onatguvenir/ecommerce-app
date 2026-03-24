Perform a security audit of the specified service or changed files.

@.claude/agents/security-auditor.md

If $ARGUMENTS specifies a service name, audit that service's `src/main/java` directory.
If $ARGUMENTS is empty, audit all files changed in the current branch (`git diff --name-only main...HEAD`).

Apply the full security auditor checklist. Pay special attention to:
1. Spring Security filter chain configuration
2. JWT validation completeness
3. Any `@RestController` endpoints — are they authenticated?
4. Input validation on all `@RequestBody` parameters
5. No secrets in `application.yml` or any committed config file

Output each finding as:
- **Severity**: Critical | High | Medium | Low
- **File**: `path/to/File.java:line`
- **Issue**: description
- **Fix**: concrete remediation
