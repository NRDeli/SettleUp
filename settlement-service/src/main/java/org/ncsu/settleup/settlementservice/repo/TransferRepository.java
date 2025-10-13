package org.ncsu.settleup.settlementservice.repo;

import org.ncsu.settleup.settlementservice.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository for managing transfer entities.
 */
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /**
     * Find transfers belonging to a particular group.
     */
    List<Transfer> findByGroupId(Long groupId);
}
