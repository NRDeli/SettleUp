package org.ncsu.settleup.settlementservice.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Transfer} entity.  These tests verify that
 * the default constructor leaves all fields null and that each setter
 * correctly assigns the provided value, with the corresponding getter
 * returning the same value.  This simple POJO has no additional logic,
 * so covering the constructors and accessors yields full coverage.
 */
public class TransferTest {

    @Test
    void defaultConstructor_leavesAllFieldsNull() {
        Transfer transfer = new Transfer();
        assertNull(transfer.getId(), "id should be null by default");
        assertNull(transfer.getGroupId(), "groupId should be null by default");
        assertNull(transfer.getFromMemberId(), "fromMemberId should be null by default");
        assertNull(transfer.getToMemberId(), "toMemberId should be null by default");
        assertNull(transfer.getAmount(), "amount should be null by default");
        assertNull(transfer.getNote(), "note should be null by default");
    }

    @Test
    void settersAndGetters_assignAndReturnValuesCorrectly() {
        Transfer transfer = new Transfer();
        Long id = 1L;
        Long groupId = 10L;
        Long fromMemberId = 2L;
        Long toMemberId = 3L;
        BigDecimal amount = new BigDecimal("42.50");
        String note = "Test payment";

        transfer.setId(id);
        transfer.setGroupId(groupId);
        transfer.setFromMemberId(fromMemberId);
        transfer.setToMemberId(toMemberId);
        transfer.setAmount(amount);
        transfer.setNote(note);

        assertEquals(id, transfer.getId());
        assertEquals(groupId, transfer.getGroupId());
        assertEquals(fromMemberId, transfer.getFromMemberId());
        assertEquals(toMemberId, transfer.getToMemberId());
        assertEquals(amount, transfer.getAmount());
        assertEquals(note, transfer.getNote());
    }
}