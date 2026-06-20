---
name: service-layer-transaction-manager
description: Dictates business logic boundaries, transactional safety, and strict exception handling for entity operations.
---

# Service Layer Transaction Manager Skill

This skill controls the core business logic, ensuring operations are transactionally safe and soft-delete protocols are respected without executing destructive database commands.

## Policies Enforced
1. **Abstraction**: Strict separation of the interface (`Service`) and the implementation (`ServiceImpl`).
2. **Data Preservation**: Hard SQL `DELETE` commands are banned.
3. **Transactional Integrity**: All write operations must be explicitly wrapped in `@Transactional`.

## Instructions

1. **Build the Interface**: Define the CRUD operations, ensuring every method signature requires the `authenticatedStudentId` integer.
2. **Implement Business Logic**: 
   - For reads: Fetch via the repository using the ID and `studentId`. If empty, throw a `ResourceNotFoundException`.
   - For writes: Map DTOs to entities and save.
3. **Execute Soft-Delete**: Implement the delete method by fetching the entity, setting `active = false`, updating the `status` if necessary, and saving. Do not call `.delete()`.
4. **Return Types**: Always return the appropriate `ResponseDTO`, never the raw Entity.