# Phase 08 — Write Isolation with JdbcTemplate and PostgreSQL RLS

## Objective

Extend tenant isolation from read operations to write operations.

Until the previous phase, PostgreSQL Row Level Security (RLS) was protecting data visibility.

This phase validates that:

* writes execute under the active tenant context
* inserts cannot bypass tenant boundaries
* runtime validates tenant presence before persistence
* PostgreSQL remains the final protection layer

The implementation intentionally keeps `JdbcTemplate` to validate behavior before introducing ORM abstractions.

---

## Problem Statement

Previous phases validated:

```text
tenant → SELECT → isolated rows
```

But write operations were not yet protected.

Without explicit controls, inserts could introduce:

```text
cross-tenant writes
or
records without tenant ownership
```

The goal became enforcing tenant isolation during creation.

---

## Architectural Flow

Write requests now follow:

```mermaid
graph TD
    %% Node Declarations
    Node_Req["HTTP Request"]
    Node_Filter["TenantFilter"]
    Node_Ctx["TenantContext"]
    Node_Val["Service Validation"]
    Node_Repo["Repository"]
    Node_DS["TenantAwareDataSource"]
    Node_Session["PostgreSQL Session Variable"]
    Node_RLS["RLS"]

    %% Relationships
    Node_Req -->|1. Transmits Header| Node_Filter
    Node_Filter -->|2. Extracts Tenant ID| Node_Ctx
    Node_Ctx -->|3. Validates Presence| Node_Val
    Node_Val -->|4. Triggers Query| Node_Repo
    Node_Repo -->|5. Requests Connection| Node_DS
    Node_DS -->|6. SET LOCAL app.current_tenant| Node_Session
    Node_Session -->|7. Evaluates Row Policy| Node_RLS

    %% Styling
    style Node_Req fill:#f9f,stroke:#333,stroke-width:2px
    style Node_Filter fill:#f96,stroke:#333,stroke-width:2px
    style Node_Ctx fill:#bbf,stroke:#333,stroke-width:2px
    style Node_Val fill:#ff9999,stroke:#333,stroke-width:2px
    style Node_Repo fill:#ffff99,stroke:#333,stroke-width:2px
    style Node_DS fill:#99bbf,stroke:#333,stroke-width:2px
    style Node_Session fill:#e1bbf7,stroke:#333,stroke-width:2px
    style Node_RLS fill:#99ff99,stroke:#333,stroke-width:2px
```

This separates responsibilities.

---

## Service Layer Introduction

A service layer was introduced between controller and repository.

Responsibilities:

* validate business rules
* define transaction boundaries
* stop invalid execution early

Implementation:

```java
@Service
public class PatientService {

    @Transactional
    public void create(
            CreatePatientRequest request
    ) {

        if (
                !StringUtils.hasText(
                        TenantContext.getTenantId()
                )
        ) {
            throw new IllegalStateException(
                    "Tenant not set"
            );
        }

        repository.create(
                request.name()
        );

    }

}
```

This introduced fail-fast validation.

---

## Repository Write Implementation

Patient creation remained intentionally simple.

```java
public static final String INSERT =
        """
        INSERT INTO patients (
            name,
            tenant_id
        )
        VALUES (
            ?,
            current_setting(
                'app.current_tenant'
            )::uuid
        )
        """;
```

Important decision:

Tenant ownership is not accepted from request payload.

Ownership is derived from database session state.

Result:

```text
request
↓
session
↓
database
```

instead of:

```text
request
↓
tenant_id
↓
database
```

---

## Why tenant_id Was Not Accepted From API

An alternative would be:

```json
{
  "name": "Charlie",
  "tenantId": "..."
}
```

This approach was intentionally rejected.

Reasons:

* ownership becomes user-controlled
* accidental cross-tenant writes become possible
* application and database may diverge

The selected model guarantees:

```text
tenant source of truth
=
database session
```

---

## Controller Changes

Controller now delegates persistence.

Example:

```java
@PostMapping("/patients")
public ResponseEntity<Void> create(
        @RequestBody
        CreatePatientRequest request
) {

    patientService.create(
            request
    );

    return ResponseEntity
            .status(
                    HttpStatus.CREATED
            )
            .build();

}
```

---

## Runtime Validation

Tenant absence now fails before SQL execution.

Integration test:

```java
@Test
void shouldRejectInsertWithoutTenant()
```

Validation:

```text
POST
without tenant
↓
IllegalStateException
↓
repository not executed
```

This replaced database-driven failures with application-level validation.

---

## Write Isolation Validation

Insert executed under Hospital A:

```text
POST /database/patients
X-Tenant-Id: HospitalA
```

Read executed under Hospital B:

```text
GET /database/patients
X-Tenant-Id: HospitalB
```

Validation:

```java
.andExpect(
    jsonPath(
        "$[*].name"
    )
    .value(
        not(
            hasItem(
                "Dave"
            )
        )
    )
);
```

Result:

```text
Hospital B
cannot read
Hospital A writes
```

---

## Defense in Depth

Protection now exists at multiple layers.

```mermaid
graph TD
    %% Node Declarations
    Node_Filter["Filter"]
    Node_Ctx["Context"]
    Node_Val["Service Validation"]
    Node_DS["Datasource"]
    Node_RLS["RLS"]

    %% Relationships
    Node_Filter -->|Layer 1: Edge Validation| Node_Ctx
    Node_Ctx -->|Layer 2: Thread Isolation| Node_Val
    Node_Val -->|Layer 3: Business Logic| Node_DS
    Node_DS -->|Layer 4: Session Invalidation| Node_RLS

    %% Styling
    style Node_Filter fill:#ff9999,stroke:#333,stroke-width:2px
    style Node_Ctx fill:#f96,stroke:#333,stroke-width:2px
    style Node_Val fill:#ffff99,stroke:#333,stroke-width:2px
    style Node_DS fill:#bbf,stroke:#333,stroke-width:2px
    style Node_RLS fill:#99ff99,stroke:#333,stroke-width:2px
```

This ensures that:

* application validates intent
* database guarantees enforcement

Even if application logic diverges, RLS remains authoritative.

---

## Key Learnings

* write isolation must be validated independently
* tenant ownership should not come from payload
* service layer improves failure semantics
* fail-fast reduces unnecessary database execution
* RLS remains valuable even after application validation
* JdbcTemplate is sufficient to validate architecture before ORM adoption

---

## Final Result

The application now supports:

```text
tenant-aware reads
+
tenant-aware writes
+
runtime validation
+
database enforcement
```

Phase completed successfully.
