package org.ncsu.settleup.common.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents the output of a settlement computation.  A settlement plan
 * consists of one or more transfers that should be executed to
 * settle outstanding balances between members of a group.  Each
 * transfer indicates the payer, the payee and the amount to be paid.
 */
public record SettlementPlan(List<TransferDto> transfers) {

    /**
     * A single transfer from one member to another.  Amounts are
     * expressed in the same currency as the associated group.
     *
     * @param fromMemberId the member who should pay
     * @param toMemberId the member who should receive payment
     * @param amount the amount to transfer
     */
    public static record TransferDto(Long fromMemberId,
                                     Long toMemberId,
                                     BigDecimal amount) {
    }
}
