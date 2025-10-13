package org.ncsu.settleup.common.dto;

/**
 * Represents a request to compute a settlement plan for a particular group.
 *
 * The groupId identifies the group whose balances should be settled.
 * The baseCurrency indicates the currency in which the settlement should be expressed.
 */
public record SettlementComputeRequest(Long groupId, String baseCurrency) {
}
