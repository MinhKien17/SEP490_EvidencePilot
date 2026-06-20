---
name: repository-tenancy-enforcer
description: Enforces strict multi-tenancy isolation and soft-delete scoping on all Spring Data JPA repositories.
---

# Repository Tenancy Enforcer Skill

This skill guarantees that no query can accidentally leak data across tenants (students) or expose deleted records.

## Policies Enforced
1. **Soft-Delete Adherence**: All read operations must filter by `active = true`.
2. **Strict Tenancy**: Global, unfiltered queries are strictly prohibited. Every query must be scoped to a specific owner.

## Instructions

1. **Extend Spring Data**: Create a repository extending `JpaRepository`.
2. **Write Scoped Finders**: Generate custom finder methods that always include the tenant ID and the active flag. 
   - *Example:* `Optional<Project> findByIdAndStudentIdAndActiveTrue(Integer id, Integer studentId);`
3. **Write Scoped Collections**: Generate list queries with the same constraints.
   - *Example:* `List<Project> findAllByStudentIdAndActiveTrue(Integer studentId);`
4. **Reject Unsafe Queries**: If asked to write a standard `findAll()` or `findById()` without the `studentId` parameter, refuse the request and cite the tenancy policy.