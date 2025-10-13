package org.ncsu.settleup.settlementservice.service;

import org.ncsu.settleup.common.dto.SettlementPlan;
import org.ncsu.settleup.common.events.ExpenseRecordedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains per-group balances based on expense events and computes
 * settlement plans.  The balances are held in-memory; if the service
 * restarts, you may need to recompute balances from persisted
 * expenses.  In a production system, this state would be stored
 * durably.
 */
@Service
public class SettlementService {
    // groupId -> (memberId -> net balance)
    private final Map<Long, Map<Long, BigDecimal>> groupBalances = new ConcurrentHashMap<>();

    /**
     * Handle an expense recorded event by updating the net balances.
     * The payer's balance increases by the total amount of the expense;
     * each participant's balance decreases by their share.
     */
    @RabbitListener(queues = "settlement.expense.recorded.queue")
    public void handleExpenseRecorded(ExpenseRecordedEvent event) {
        Map<Long, BigDecimal> balances = groupBalances.computeIfAbsent(event.groupId(), k -> new ConcurrentHashMap<>());

        // Compute total of shares
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal share : event.shares().values()) {
            total = total.add(share);
        }
        // Payer gets credit for paying the total
        balances.merge(event.payerMemberId(), total, BigDecimal::add);
        // Each member owes their share
        for (Map.Entry<Long, BigDecimal> entry : event.shares().entrySet()) {
            Long memberId = entry.getKey();
            BigDecimal amount = entry.getValue();
            balances.merge(memberId, amount.negate(), BigDecimal::add);
        }
    }

    /**
     * Compute a settlement plan by pairing debtors and creditors.  This
     * implementation uses a greedy algorithm: sort creditors (positive
     * balances) and debtors (negative balances) and settle the
     * largest debts first.
     *
     * @param groupId the group whose balances should be settled
     * @return a plan consisting of transfers to settle all balances
     */
    public SettlementPlan computeSettlement(Long groupId) {
        Map<Long, BigDecimal> balances = groupBalances.getOrDefault(groupId, Map.of());
        // Separate creditors and debtors
        List<Map.Entry<Long, BigDecimal>> creditors = new ArrayList<>();
        List<Map.Entry<Long, BigDecimal>> debtors = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(Map.entry(entry.getKey(), entry.getValue()));
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(Map.entry(entry.getKey(), entry.getValue().negate()));
            }
        }
        // Sort by balance descending
        creditors.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        debtors.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        List<SettlementPlan.TransferDto> transfers = new ArrayList<>();
        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<Long, BigDecimal> debtor = debtors.get(i);
            Map.Entry<Long, BigDecimal> creditor = creditors.get(j);
            BigDecimal debt = debtor.getValue();
            BigDecimal credit = creditor.getValue();
            BigDecimal amount = debt.min(credit);
            transfers.add(new SettlementPlan.TransferDto(debtor.getKey(), creditor.getKey(), amount));
            // update remaining amounts
            BigDecimal newDebt = debt.subtract(amount);
            BigDecimal newCredit = credit.subtract(amount);
            if (newDebt.compareTo(BigDecimal.ZERO) == 0) {
                i++;
            } else {
                debtors.set(i, Map.entry(debtor.getKey(), newDebt));
            }
            if (newCredit.compareTo(BigDecimal.ZERO) == 0) {
                j++;
            } else {
                creditors.set(j, Map.entry(creditor.getKey(), newCredit));
            }
        }
        return new SettlementPlan(transfers);
    }

    /**
     * Apply a transfer to the in-memory balances.  When a member pays
     * another, the debtor's balance increases by the amount and the
     * creditor's balance decreases.
     *
     * @param groupId the group identifier
     * @param fromMemberId the member who paid (debtor)
     * @param toMemberId the member who received payment (creditor)
     * @param amount the amount transferred
     */
    public void applyTransfer(Long groupId, Long fromMemberId, Long toMemberId, BigDecimal amount) {
        Map<Long, BigDecimal> balances = groupBalances.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>());
        // The debtor's balance increases (less owed)
        balances.merge(fromMemberId, amount, BigDecimal::add);
        // The creditor's balance decreases (less to collect)
        balances.merge(toMemberId, amount.negate(), BigDecimal::add);
    }
}
