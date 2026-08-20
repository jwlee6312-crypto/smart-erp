# Implementation Plan - Restore Missing DTOs for erp-mobile-backend

The `erp-mobile-backend` project is currently failing to start because several DTO (Data Transfer Object) classes referenced in MyBatis XML mappers are missing. These DTOs were likely deleted during a cleanup process. This plan restores the necessary DTOs from the `erp-backend` project and adapts them for the mobile backend.

## User Review Required

> [!IMPORTANT]
> I will restore 10 DTO classes from `erp-backend` to `erp-mobile-backend`. I will ensure the package names are updated to include `.mobile`.

## Proposed Changes

### [erp-mobile-backend]

I will restore the following DTOs to their respective packages in `src/main/java/com/crmbank/erp/mobile/`.

#### [NEW] PdInspClsfyDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hpio/dto/PdInspClsfyDto.java`
- **Source:** `erp-backend/.../hpio/dto/PdInspClsfyDto.java`

#### [NEW] PdInspErrTypeDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hpio/dto/PdInspErrTypeDto.java`
- **Source:** `erp-backend/.../hpio/dto/PdInspErrTypeDto.java`

#### [NEW] PdInspItemStdDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hpio/dto/PdInspItemStdDto.java`
- **Source:** `erp-backend/.../hpio/dto/PdInspItemStdDto.java`

#### [NEW] PdInspJudgDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hpio/dto/PdInspJudgDto.java`
- **Source:** `erp-backend/.../hpio/dto/PdInspJudgDto.java`

#### [NEW] PdInspReqDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hpio/dto/PdInspReqDto.java`
- **Source:** `erp-backend/.../hpio/dto/PdInspReqDto.java`

#### [NEW] PdInspRsltDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hpio/dto/PdInspRsltDto.java`
- **Source:** `erp-backend/.../hpio/dto/PdInspRsltDto.java`

#### [NEW] PdInspRsltDtlDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hpio/dto/PdInspRsltDtlDto.java`
- **Source:** `erp-backend/.../hpio/dto/PdInspRsltDtlDto.java`

#### [NEW] PdCapaDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hppl/dto/PdCapaDto.java`
- **Source:** `erp-backend/.../hppl/dto/PdCapaDto.java`

#### [NEW] PdOrderDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hppl/dto/PdOrderDto.java`
- **Source:** `erp-backend/.../hppl/dto/PdOrderDto.java`

#### [NEW] PdPlanDto.java
- **Path:** `src/main/java/com/crmbank/erp/mobile/hppl/dto/PdPlanDto.java`
- **Source:** `erp-backend/.../hppl/dto/PdPlanDto.java`

## Verification Plan

### Automated Tests
- Run `./gradlew :erp-mobile-backend:compileJava` to ensure all restored DTOs are correctly referenced and the project compiles.

### Manual Verification
- Verify that the `erp-mobile-backend` application starts successfully without `ClassNotFoundException` or `UnsatisfiedDependencyException` related to MyBatis type aliases.
