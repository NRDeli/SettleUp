package org.ncsu.settleup.expenseservice.repo;

import org.ncsu.settleup.expenseservice.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository for managing Expense entities.
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Find all expenses belonging to a particular group.
     *
     * @param groupId group identifier
     * @return list of expenses
     */
    List<Expense> findByGroupId(Long groupId);
}
