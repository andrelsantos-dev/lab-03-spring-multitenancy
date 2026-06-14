# Lab 03 - Spring Multitenancy

Hands-on laboratory to explore multitenancy implementation in Spring Boot integrated with PostgreSQL Row Level Security (RLS).

This lab builds on the concepts explored in Lab 02 and focuses on application-level tenant context propagation and database integration.

## Related Project

- Lab 01 - Flyway + Testcontainers
- Lab 02 - PostgreSQL Row Level Security (RLS)

This lab assumes familiarity with the concepts explored in Lab 02:

- PostgreSQL Row Level Security (RLS)
- Tenant isolation
- Session variables
- PostgreSQL roles and policies

## Goal

Build a Spring Boot application that propagates tenant context and relies on PostgreSQL RLS as the primary tenant isolation mechanism.

## Learning Path

| Phase      |Topic|
|------------|---|
| ✅ Phase 01 | Bootstrap |
| ✅ Phase 02 | Tenant Context Propagation |
| ✅ Phase 03   | Database Connectivity |
| ✅ Phase 04   | Session Variables |
| ✅ Phase 05   | Tenant-Aware Data Access |
| ✅ Phase 06   | RLS Integration |
| ✅ Phase 07   | Integration Tests |
| Phase 08   | Production Considerations |

## Prerequisites

- Java 21
- Maven
- Docker


## Running PostgreSQL

```bash
make up
```

## Running Spring Boot

```bash
make run
```

## List of available commands

```bash
make
```


## References

- [AWS SaaS Factory PostgreSQL RLS](https://github.com/aws-samples/aws-saas-factory-postgresql-rls)
- [Multi-tenant data isolation with PostgreSQL Row Level Security](https://aws.amazon.com/pt/blogs/database/multi-tenant-data-isolation-with-postgresql-row-level-security/)
- [AWS Prescriptive Guidance](https://docs.aws.amazon.com/prescriptive-guidance/latest/saas-multitenant-managed-postgresql/rls.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Multitenancy With Spring Data JPA](https://www.baeldung.com/multitenancy-with-spring-data-jpa)
- [Guidance for using Azure Database for PostgreSQL in a multitenant solution](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/service/postgresql)