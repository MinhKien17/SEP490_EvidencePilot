---
name: jpa-domain-architect
description: Enforces strict data modeling, Jakarta Validation, and exact MySQL schema alignment for all Spring Boot entities and DTOs.
---

# JPA Domain Architect Skill

This skill ensures that all Java Domain models strictly mirror the underlying database schema and prevents the agent from generating unsafe entity relationships or unvalidated DTOs.

## Policies Enforced
1. **Schema Fidelity**: Java Enums must exactly match MySQL `ENUM` constraints. 
2. **Performance Safety**: All `@ManyToOne` and `@OneToMany` relationships must explicitly declare `FetchType.LAZY`.
3. **Input Security**: DTOs are mandatory. Domain entities must never be used directly as HTTP request/response payloads.

## Instructions

1. **Map the Entity**: Generate the JPA Entity matching the provided SQL schema exactly. Map `id` to the primary key. Do not invent columns that do not exist in the schema.
2. **Implement DTOs**: Create `Create`, `Update`, and `Response` DTOs. 
3. **Enforce Jakarta Validation**: Apply strict validation annotations (`@NotBlank`, `@NotNull`, `@Size`) to all fields in the `Create` and `Update` DTOs.
4. **Halt and Verify**: Stop execution and output the generated code. Do not proceed to the repository layer until the user approves the domain models.