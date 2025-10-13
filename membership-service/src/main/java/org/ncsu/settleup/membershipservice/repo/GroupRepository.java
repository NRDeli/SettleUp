package org.ncsu.settleup.membershipservice.repo;

import org.ncsu.settleup.membershipservice.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing GroupEntity instances.  Spring Data JPA
 * automatically implements the CRUD operations.
 */
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {
}
