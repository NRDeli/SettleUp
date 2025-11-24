package org.ncsu.settleup.settlementservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ncsu.settleup.common.dto.SettlementPlan;
import org.ncsu.settleup.common.events.ExpenseRecordedEvent;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional unit tests for {@link SettlementService} that exercise all
 * branches in {@code handleExpenseRecorded}, {@code applyTransfer} and
 * {@code computeSettlement}.  These tests operate directly on a fresh
 * {@link SettlementService} instance without involving Spring, using
 * reflection to inspect and seed the private {@code groupBalances} map
 * where necessary.  Collectively with the existing black‑box tests,
 * they should bring coverage of the service class to 100%.
 */
public class SettlementServiceTest {

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService();
    }

    /**
     * Reflection helper to access the private groupBalances field on a
     * {@link SettlementService} instance.  Used to verify internal state
     * after method calls.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Map<Long, BigDecimal>> getBalances() throws Exception {
        Field f = SettlementService.class.getDeclaredField("groupBalances");
        f.setAccessible(true);
        return (Map<Long, Map<Long, BigDecimal>>) f.get(settlementService);
    }

    /**
     * Helper to seed balances for a group.  Creates a new ConcurrentHashMap
     * entry for the group with a copy of the provided balances.
     */
    private void seedBalances(Long groupId, Map<Long, BigDecimal> balances) throws Exception {
        Map<Long, Map<Long, BigDecimal>> all = getBalances();
        if (all == null) {
            all = new ConcurrentHashMap<>();
            Field f = SettlementService.class.getDeclaredField("groupBalances");
            f.setAccessible(true);
            f.set(settlementService, all);
        }
        all.put(groupId, new ConcurrentHashMap<>(balances));
    }

    // ---------------------------------------------------------------------
    // Tests for handleExpenseRecorded
    // ---------------------------------------------------------------------

    @Test
    void handleExpenseRecorded_createsGroupAndUpdatesBalances() throws Exception {
        // Event for new group: payer 2 pays total of 5 (3+2) for members 1 and 3
        Map<Long, BigDecimal> shares = new HashMap<>();
        shares.put(1L, new BigDecimal("3"));
        shares.put(3L, new BigDecimal("2"));
        ExpenseRecordedEvent event = new ExpenseRecordedEvent(1L, 42L, 2L, shares);
        settlementService.handleExpenseRecorded(event);
        Map<Long, BigDecimal> groupMap = getBalances().get(1L);
        assertNotNull(groupMap, "Group should be created");
        assertEquals(new BigDecimal("5"), groupMap.get(2L), "Payer should be credited total of shares");
        assertEquals(new BigDecimal("-3"), groupMap.get(1L));
        assertEquals(new BigDecimal("-2"), groupMap.get(3L));
    }

    @Test
    void handleExpenseRecorded_accumulatesBalancesForExistingGroup() throws Exception {
        // Seed existing balances for group 2
        seedBalances(2L, Map.of(
                5L, new BigDecimal("4"),
                6L, new BigDecimal("-1")
        ));
        // First event: payer 5 pays 3 for member 7
        Map<Long, BigDecimal> shares1 = Map.of(7L, new BigDecimal("3"));
        settlementService.handleExpenseRecorded(new ExpenseRecordedEvent(2L, 100L, 5L, shares1));
        // Second event: payer 6 pays 2 for member 5
        Map<Long, BigDecimal> shares2 = Map.of(5L, new BigDecimal("2"));
        settlementService.handleExpenseRecorded(new ExpenseRecordedEvent(2L, 101L, 6L, shares2));
        Map<Long, BigDecimal> map = getBalances().get(2L);
        // Balances should reflect initial seed plus events:
        // seed: 5 -> 4, 6 -> -1
        // event1: 5 credited +3 (new 7); 7 debited -3
        // event2: 6 credited +2 (new 1); 5 debited -2 (new 5)
        assertEquals(new BigDecimal("5"), map.get(5L));
        assertEquals(new BigDecimal("1"), map.get(6L));
        assertEquals(new BigDecimal("-3"), map.get(7L));
    }

    // ---------------------------------------------------------------------
    // Tests for applyTransfer
    // ---------------------------------------------------------------------

    @Test
    void applyTransfer_createsGroupIfMissing() throws Exception {
        settlementService.applyTransfer(3L, 1L, 2L, new BigDecimal("5"));
        Map<Long, BigDecimal> balances = getBalances().get(3L);
        assertNotNull(balances, "Group should be created");
        assertEquals(new BigDecimal("5"), balances.get(1L));
        assertEquals(new BigDecimal("-5"), balances.get(2L));
    }

    @Test
    void applyTransfer_updatesExistingBalances() throws Exception {
        // Seed group 4 with some balances
        seedBalances(4L, Map.of(
                9L, new BigDecimal("-2"),
                8L, new BigDecimal("2")
        ));
        // Debtor 9 pays creditor 8 amount 4
        settlementService.applyTransfer(4L, 9L, 8L, new BigDecimal("4"));
        Map<Long, BigDecimal> map = getBalances().get(4L);
        // Debtor's balance increases (-2 + 4 = 2); creditor's balance decreases (2 - 4 = -2)
        assertEquals(new BigDecimal("2"), map.get(9L));
        assertEquals(new BigDecimal("-2"), map.get(8L));
    }

    // ---------------------------------------------------------------------
    // Tests for computeSettlement covering corner cases
    // ---------------------------------------------------------------------

    @Test
    void computeSettlement_returnsEmptyPlanWhenNoCreditors() throws Exception {
        // All balances <= 0
        seedBalances(5L, Map.of(
                1L, new BigDecimal("0"),
                2L, new BigDecimal("-3"),
                3L, new BigDecimal("-2")
        ));
        SettlementPlan plan = settlementService.computeSettlement(5L);
        assertTrue(plan.transfers().isEmpty(), "No transfers expected when there are no creditors");
    }

    @Test
    void computeSettlement_returnsEmptyPlanWhenNoDebtors() throws Exception {
        // All balances >= 0
        seedBalances(6L, Map.of(
                10L, new BigDecimal("3"),
                11L, new BigDecimal("0"),
                12L, new BigDecimal("2")
        ));
        SettlementPlan plan = settlementService.computeSettlement(6L);
        assertTrue(plan.transfers().isEmpty(), "No transfers expected when there are no debtors");
    }

    @Test
    void computeSettlement_handlesZeroBalancesGracefully() throws Exception {
        seedBalances(7L, Map.of(
                20L, BigDecimal.ZERO,
                21L, BigDecimal.ZERO
        ));
        SettlementPlan plan = settlementService.computeSettlement(7L);
        assertTrue(plan.transfers().isEmpty(), "No transfers expected when all balances are zero");
    }

    @Test
    void computeSettlement_multipleCreditorsAndDebtorsProducesCorrectTransfers() throws Exception {
        // Setup: creditors 1 (+10), 2 (+5); debtors 3 (-8), 4 (-7)
        seedBalances(8L, Map.of(
                1L, new BigDecimal("10"),
                2L, new BigDecimal("5"),
                3L, new BigDecimal("-8"),
                4L, new BigDecimal("-7")
        ));
        SettlementPlan plan = settlementService.computeSettlement(8L);
        List<SettlementPlan.TransferDto> transfers = plan.transfers();
        assertEquals(3, transfers.size(), "There should be three transfers in this scenario");
        // Build a set of strings representing each transfer for easy assertion
        Set<String> transferSet = transfers.stream()
                .map(t -> t.fromMemberId() + ":" + t.toMemberId() + ":" + t.amount())
                .collect(Collectors.toSet());
        assertTrue(transferSet.contains("3:1:8"), "Expected transfer of 8 from member 3 to 1");
        assertTrue(transferSet.contains("4:1:2"), "Expected transfer of 2 from member 4 to 1");
        assertTrue(transferSet.contains("4:2:5"), "Expected transfer of 5 from member 4 to 2");
    }
}