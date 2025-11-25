package org.ncsu.settleup.settlementservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ncsu.settleup.common.dto.SettlementComputeRequest;
import org.ncsu.settleup.common.dto.SettlementPlan;
import org.ncsu.settleup.settlementservice.client.MembershipClient;
import org.ncsu.settleup.settlementservice.model.Transfer;
import org.ncsu.settleup.settlementservice.repo.TransferRepository;
import org.ncsu.settleup.settlementservice.service.SettlementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link SettlementController}. These tests exercise the controller logic
 * without starting a Spring MVC container. All dependencies are mocked and the controller
 * methods are invoked directly.
 */
@ExtendWith(MockitoExtension.class)
class SettlementControllerTest {

    @Mock
    private SettlementService settlementService;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private MembershipClient membershipClient;

    @InjectMocks
    private SettlementController controller;

    @BeforeEach
    void setUp() {
        // Mockito will inject mocks into the controller; no additional setup required here.
    }

    @Test
    void computeSettlement_groupNotFound_returnsNotFound() {
        Long groupId = 100L;
        SettlementComputeRequest req = new SettlementComputeRequest(groupId, "USD");
        when(membershipClient.groupExists(groupId)).thenReturn(false);

        ResponseEntity<?> resp = controller.computeSettlement(req);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Group not found", resp.getBody());
        verify(membershipClient).groupExists(groupId);
        verifyNoInteractions(settlementService);
    }

    @Test
    void computeSettlement_groupExists_returnsPlan() {
        Long groupId = 101L;
        SettlementComputeRequest req = new SettlementComputeRequest(groupId, "USD");
        SettlementPlan plan = new SettlementPlan(List.of());

        when(membershipClient.groupExists(groupId)).thenReturn(true);
        when(settlementService.computeSettlement(groupId)).thenReturn(plan);

        ResponseEntity<?> resp = controller.computeSettlement(req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(plan, resp.getBody());
        verify(membershipClient).groupExists(groupId);
        verify(settlementService).computeSettlement(groupId);
    }

    @Test
    void recordTransfer_groupNotFound_returnsNotFound() {
        Long groupId = 10L;
        SettlementController.TransferRequest request =
                new SettlementController.TransferRequest(groupId, 1L, 2L, BigDecimal.TEN, "test");

        when(membershipClient.groupExists(groupId)).thenReturn(false);

        ResponseEntity<?> resp = controller.recordTransfer(request);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Group not found", resp.getBody());
        verify(membershipClient).groupExists(groupId);
        verify(membershipClient, never()).memberExists(any(), any());
        verifyNoInteractions(transferRepository, settlementService);
    }

    @Test
    void recordTransfer_invalidMembers_returnsBadRequest() {
        Long groupId = 20L;
        Long fromId = 1L;
        Long toId = 2L;
        SettlementController.TransferRequest request =
                new SettlementController.TransferRequest(groupId, fromId, toId, BigDecimal.TEN, "bad");

        when(membershipClient.groupExists(groupId)).thenReturn(true);
        // One of the member lookups will return false
        when(membershipClient.memberExists(groupId, fromId)).thenReturn(true);
        when(membershipClient.memberExists(groupId, toId)).thenReturn(false);

        ResponseEntity<?> resp = controller.recordTransfer(request);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("From or To member does not exist or is not part of the group", resp.getBody());

        verify(membershipClient).groupExists(groupId);
        verify(membershipClient).memberExists(groupId, fromId);
        verify(membershipClient).memberExists(groupId, toId);
        verifyNoInteractions(transferRepository, settlementService);
    }

    @Test
    void recordTransfer_success_returnsCreatedAndSaves() {
        Long groupId = 30L;
        Long fromId = 3L;
        Long toId = 4L;
        BigDecimal amount = new BigDecimal("5.00");
        String note = "settle";

        SettlementController.TransferRequest request =
                new SettlementController.TransferRequest(groupId, fromId, toId, amount, note);

        when(membershipClient.groupExists(groupId)).thenReturn(true);
        when(membershipClient.memberExists(groupId, fromId)).thenReturn(true);
        when(membershipClient.memberExists(groupId, toId)).thenReturn(true);

        Transfer saved = new Transfer();
        saved.setId(99L);
        saved.setGroupId(groupId);
        saved.setFromMemberId(fromId);
        saved.setToMemberId(toId);
        saved.setAmount(amount);
        saved.setNote(note);

        when(transferRepository.save(any(Transfer.class))).thenReturn(saved);

        ResponseEntity<?> resp = controller.recordTransfer(request);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertSame(saved, resp.getBody());

        // Capture the transfer passed to save() to verify the fields are set
        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).save(transferCaptor.capture());
        Transfer passed = transferCaptor.getValue();
        assertEquals(groupId, passed.getGroupId());
        assertEquals(fromId, passed.getFromMemberId());
        assertEquals(toId, passed.getToMemberId());
        assertEquals(amount, passed.getAmount());
        assertEquals(note, passed.getNote());

        verify(settlementService).applyTransfer(groupId, fromId, toId, amount);
    }

    @Test
    void getTransfer_notFound_returnsNotFound() {
        Long id = 42L;
        when(transferRepository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<Transfer> resp = controller.getTransfer(id);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(transferRepository).findById(id);
    }

    @Test
    void getTransfer_found_returnsTransfer() {
        Long id = 43L;
        Transfer transfer = new Transfer();
        transfer.setId(id);
        when(transferRepository.findById(id)).thenReturn(Optional.of(transfer));

        ResponseEntity<Transfer> resp = controller.getTransfer(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(transfer, resp.getBody());
        verify(transferRepository).findById(id);
    }

    @Test
    void updateTransfer_notFound_returnsNotFound() {
        Long id = 50L;
        SettlementController.TransferRequest request =
                new SettlementController.TransferRequest(1L, 2L, 3L, BigDecimal.ONE, "note");

        when(transferRepository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.updateTransfer(id, request);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Transfer not found", resp.getBody());
        verify(transferRepository).findById(id);
        verifyNoInteractions(settlementService);
    }

    @Test
    void updateTransfer_groupNotFound_returnsNotFound() {
        Long id = 60L;
        Transfer existing = new Transfer();
        existing.setId(id);
        existing.setGroupId(5L);
        existing.setFromMemberId(1L);
        existing.setToMemberId(2L);
        existing.setAmount(BigDecimal.TEN);
        existing.setNote("old");

        when(transferRepository.findById(id)).thenReturn(Optional.of(existing));

        // membershipClient.groupExists will be called with request.groupId
        Long newGroupId = 7L;
        SettlementController.TransferRequest request =
                new SettlementController.TransferRequest(newGroupId, 3L, 4L, BigDecimal.ONE, "new");
        when(membershipClient.groupExists(newGroupId)).thenReturn(false);

        ResponseEntity<?> resp = controller.updateTransfer(id, request);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Group not found", resp.getBody());
        verify(membershipClient).groupExists(newGroupId);
        verifyNoMoreInteractions(membershipClient);
        // findById should be invoked on the repository, but nothing else
        verify(transferRepository).findById(id);
        verifyNoMoreInteractions(transferRepository);
        verifyNoInteractions(settlementService);
    }

    @Test
    void updateTransfer_invalidMembers_returnsBadRequest() {
        Long id = 61L;
        Transfer existing = new Transfer();
        existing.setId(id);
        existing.setGroupId(5L);
        existing.setFromMemberId(1L);
        existing.setToMemberId(2L);
        existing.setAmount(new BigDecimal("5"));
        existing.setNote("old");

        when(transferRepository.findById(id)).thenReturn(Optional.of(existing));

        Long newGroupId = 9L;
        Long newFromId = 10L;
        Long newToId = 11L;
        BigDecimal newAmount = new BigDecimal("7");
        String newNote = "new";
        SettlementController.TransferRequest request =
                new SettlementController.TransferRequest(newGroupId, newFromId, newToId, newAmount, newNote);

        when(membershipClient.groupExists(newGroupId)).thenReturn(true);
        when(membershipClient.memberExists(newGroupId, newFromId)).thenReturn(false);
        // The second memberExists call is short-circuited because the first returned false

        ResponseEntity<?> resp = controller.updateTransfer(id, request);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("From or To member does not exist or is not part of the group", resp.getBody());

        verify(membershipClient).groupExists(newGroupId);
        verify(membershipClient).memberExists(newGroupId, newFromId);
        // findById should be invoked on the repository, but no save/delete
        verify(transferRepository).findById(id);
        verifyNoMoreInteractions(transferRepository);
        verifyNoInteractions(settlementService);
    }

    @Test
    void updateTransfer_success_updatesAndReappliesBalances() {
        Long id = 62L;
        // Existing transfer in repository
        Transfer existing = new Transfer();
        existing.setId(id);
        existing.setGroupId(5L);
        existing.setFromMemberId(1L);
        existing.setToMemberId(2L);
        existing.setAmount(new BigDecimal("3"));
        existing.setNote("old");

        when(transferRepository.findById(id)).thenReturn(Optional.of(existing));

        Long newGroupId = 6L;
        Long newFromId = 3L;
        Long newToId = 4L;
        BigDecimal newAmount = new BigDecimal("4");
        String newNote = "new note";
        SettlementController.TransferRequest request =
                new SettlementController.TransferRequest(newGroupId, newFromId, newToId, newAmount, newNote);

        when(membershipClient.groupExists(newGroupId)).thenReturn(true);
        when(membershipClient.memberExists(newGroupId, newFromId)).thenReturn(true);
        when(membershipClient.memberExists(newGroupId, newToId)).thenReturn(true);

        // save returns the updated transfer
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> resp = controller.updateTransfer(id, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Transfer saved = (Transfer) resp.getBody();

        // There should be two balance updates (reverse + new)
        verify(settlementService, times(2)).applyTransfer(any(), any(), any(), any());

        // Ensure the *new* transfer was applied with correct arguments
        verify(settlementService).applyTransfer(newGroupId, newFromId, newToId, newAmount);

        // Membership checks
        verify(membershipClient).groupExists(newGroupId);
        verify(membershipClient).memberExists(newGroupId, newFromId);
        verify(membershipClient).memberExists(newGroupId, newToId);

        // Check the saved transfer has updated values
        assertEquals(newGroupId, saved.getGroupId());
        assertEquals(newFromId, saved.getFromMemberId());
        assertEquals(newToId, saved.getToMemberId());
        assertEquals(newAmount, saved.getAmount());
        assertEquals(newNote, saved.getNote());

        // Repository interactions: findById + save
        ArgumentCaptor<Transfer> updatedCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).findById(id);
        verify(transferRepository).save(updatedCaptor.capture());
        Transfer passed = updatedCaptor.getValue();
        assertEquals(newGroupId, passed.getGroupId());
        assertEquals(newFromId, passed.getFromMemberId());
        assertEquals(newToId, passed.getToMemberId());
        assertEquals(newAmount, passed.getAmount());
        assertEquals(newNote, passed.getNote());
        verifyNoMoreInteractions(transferRepository);
    }


    @Test
    void deleteTransfer_notFound_returnsNotFound() {
        Long id = 70L;
        when(transferRepository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<String> resp = controller.deleteTransfer(id);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Transfer not found", resp.getBody());
        verify(transferRepository).findById(id);
        verifyNoInteractions(settlementService);
    }

    @Test
    void deleteTransfer_success_reversesAndDeletes() {
        Long id = 71L;
        Transfer existing = new Transfer();
        existing.setId(id);
        existing.setGroupId(5L);
        existing.setFromMemberId(1L);
        existing.setToMemberId(2L);
        existing.setAmount(new BigDecimal("9"));
        existing.setNote("del");

        when(transferRepository.findById(id)).thenReturn(Optional.of(existing));

        ResponseEntity<String> resp = controller.deleteTransfer(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Transfer deleted successfully", resp.getBody());

        verify(settlementService).applyTransfer(existing.getGroupId(), existing.getToMemberId(), existing.getFromMemberId(), existing.getAmount());
        verify(transferRepository).delete(existing);
    }

    @Test
    void getTransfersForGroup_groupNotFound_returnsNotFound() {
        Long groupId = 80L;
        when(membershipClient.groupExists(groupId)).thenReturn(false);

        ResponseEntity<?> resp = controller.getTransfersForGroup(groupId);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Group not found", resp.getBody());
        verify(membershipClient).groupExists(groupId);
        verifyNoInteractions(transferRepository);
    }

    @Test
    void getTransfersForGroup_groupExists_returnsList() {
        Long groupId = 81L;
        when(membershipClient.groupExists(groupId)).thenReturn(true);

        Transfer t1 = new Transfer();
        t1.setId(1L);
        Transfer t2 = new Transfer();
        t2.setId(2L);
        List<Transfer> list = List.of(t1, t2);
        when(transferRepository.findByGroupId(groupId)).thenReturn(list);

        ResponseEntity<?> resp = controller.getTransfersForGroup(groupId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(list, resp.getBody());
        verify(membershipClient).groupExists(groupId);
        verify(transferRepository).findByGroupId(groupId);
    }
}