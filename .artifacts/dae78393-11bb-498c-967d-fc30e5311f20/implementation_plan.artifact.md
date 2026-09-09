# Fix Asterisk Database Connectivity for HGPA Modules

The HGPA010U through HGPA040U modules are failing to query the Asterisk database due to a configuration mismatch between the backend and the Docker environment. Specifically, the database password and connection settings in `AsteriskDataSourceConfig.java` need to be aligned with `docker-compose.yml`.

## User Review Required

> [!IMPORTANT]
> The password in `docker-compose.yml` uses `$$` (e.g., `gkdldhs12#$$`). In Docker Compose YAML, `$$` is an escape sequence for a single `$`. Therefore, the actual password set in the database and passed as an environment variable is `gkdldhs12#$`.
> I will ensure the backend code correctly handles this and uses the environment variable `ASTERISK_DB_PASSWORD` as the primary source, with the correct fallback.

## Proposed Changes

### Backend Configuration

#### [MODIFY] [AsteriskDataSourceConfig.java](file:///D:/erp.crmbank.co.kr/erp-backend/src/main/java/com/crmbank/erp/config/AsteriskDataSourceConfig.java)
- Update `asteriskDataSource` to prioritize environment variables for all connection parameters.
- Ensure the password fallback and logic match the Docker environment.
- Maintain the current `MapperScan` configuration as requested.

## Verification Plan

### Manual Verification
1. Apply changes to `AsteriskDataSourceConfig.java`.
2. Rebuild the backend: `docker compose up -d --build smart-erp-backend`.
3. Access the HGPA modules (e.g., HGPA010U) in the web UI.
4. Perform a search (조회) and verify that the data is correctly retrieved from the Asterisk database.
