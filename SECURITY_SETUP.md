# Local database configuration

The three database-backed services read `DB_PASSWORD` from the process environment
without a committed fallback. `DB_USERNAME` defaults to the local `postgres` user.
Set both before launching services from a terminal or IDE. A root `.env` file is
not automatically loaded by these services; use actual environment variables or
your deployment's secret/configuration mechanism.

Use your own local database password and rotate the old value if reused outside
an isolated development database. Preserve each service's separate database URL.
The project README explains database creation and service startup without
publishing a reusable password.

Agent instructions and the internal implementation backlog were removed from
the current source tree and ignored to prevent accidental re-addition. Existing
application code, Angular editor/MCP configuration and Git history remain intact.
