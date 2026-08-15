# Modernization Plan: career-planner Azure Migration Plan

**Project**: career-planner

---

## Technical Framework

- **Language**: Java — assessment detected JDK 21; `career-core/pom.xml` now targets Java 25 (latest LTS). Companion Python 3.14 FastAPI service (`career-ai`) is integrated over HTTP.
- **Framework**: Spring Boot 3.5.16 (Spring)
- **Build Tool**: Maven 3.9.16
- **Database**: MySQL (`career_core`) — configured in `career-core/src/main/resources/application.yml` and `deploy/docker-compose.yml`
- **Key Dependencies**: Spring Boot Starters (Web, JDBC/JdbcTemplate), `mysql-connector-j`; `career-ai` integration via Spring `RestClient` (`AiExplainClient`)

---

## Overview

> This migration prepares the career-planner application for deployment on Azure. The application currently runs a Spring Boot backend (`career-core`) that uses a local MySQL database, keeps a database password default in its configuration file, and makes an unsecured HTTP call to a locally hosted FastAPI service (`career-ai` at `http://127.0.0.1:8000`). The new architecture will:
>
> - Migrate the MySQL database to Azure Database for MySQL with secure, passwordless (managed identity) authentication.
> - Secure credentials by moving plaintext secrets out of configuration into Azure Key Vault.
> - Replace unsecured HTTP calls with HTTPS and externalize hardcoded URLs and IP addresses.
> - Migrate the locally hosted `career-ai` resource to an Azure-hosted endpoint.
> - Complete the Java runtime upgrade to the latest LTS (Java 25).
>
> The migration follows a phased approach: upgrade the runtime first, then transform the data layer, security, and network configuration to make the application Azure-ready.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| career-core | Local MySQL (career_core) | Azure Database for MySQL | Managed Identity | Assessment: MySQL database found |
| career-core | Plaintext password in application.yml | Azure Key Vault | Managed Identity | Assessment: password in config file |
| career-core | HTTP call to career-ai (127.0.0.1) | Azure-hosted HTTPS endpoint | Managed Identity | Assessment: unsecure protocol / URL / IP / localhost |
| career-core | Java 21 runtime | Java 25 LTS | N/A | Assessment: Java version not latest LTS |

---

## Planned Tasks

The detailed, machine-readable task breakdown lives in `.metadata/tasks.json`. High-level scope (7 tasks, one per selected assessment category):

| # | Task | Type |
|---|------|------|
| 1 | Upgrade Java Version to latest LTS (Java 25) | upgrade |
| 2 | Migrate to Azure Database for MySQL | transform |
| 3 | Migrate from Plaintext Credentials to Azure Key Vault | transform |
| 4 | Use Secure Protocols | transform |
| 5 | Check hardcoded URLs | transform |
| 6 | Migrate the Local Resource to Azure | transform |
| 7 | Check hardcoded IP address | transform |

---

## Open Questions & Questionnaire

- [x] Q: Should the plan include infrastructure provisioning? → A: No — migration/code-readiness only; the plan is scoped to the 7 selected assessment categories.
- [x] Q: Should the plan include a security/CVE remediation task? → A: No — not among the selected categories, and the assessment report contains no security/CVE findings (`"security": []`).
- [x] Q: Which Azure deployment target should the plan use? → A: No deployment requested — migration only (Azure Database for MySQL, Azure Key Vault, and an Azure-hosted endpoint for `career-ai` are handled as code/transform tasks).
- [x] Q: Should the plan include containerization? → A: No — no deployment target requested.
