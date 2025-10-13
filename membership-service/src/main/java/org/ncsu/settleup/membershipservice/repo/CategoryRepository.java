package org.ncsu.settleup.membershipservice.repo;

import org.ncsu.settleup.membershipservice.model.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing CategoryEntity instances.
 */
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
}
