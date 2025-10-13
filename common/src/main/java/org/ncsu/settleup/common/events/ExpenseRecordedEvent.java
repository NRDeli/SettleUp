package org.ncsu.settleup.common.events;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Event published by the expense service whenever a new expense is
 * recorded.  The settlement service listens for this event and
 * updates the in-memory balances accordingly.
 *
 * @param groupId        the ID of the group associated with the expense
 * @param expenseId      the unique identifier of the newly created expense
 * @param payerMemberId  the ID of the member who paid the expense
 * @param shares         a map of member IDs to the amount they owe.  The map
 *                       does not include the payer; shares should sum to
 *                       the total amount of the expense.
 */
public record ExpenseRecordedEvent(Long groupId,
                                   Long expenseId,
                                   Long payerMemberId,
                                   Map<Long, BigDecimal> shares) {
}
