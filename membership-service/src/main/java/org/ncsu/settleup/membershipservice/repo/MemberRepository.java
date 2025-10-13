package org.ncsu.settleup.membershipservice.repo;

import org.ncsu.settleup.membershipservice.model.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing MemberEntity instances.
 */
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
}
