package org.ncsu.settleup.settlementservice.controller;

import org.ncsu.settleup.common.dto.SettlementComputeRequest;
import org.ncsu.settleup.common.dto.SettlementPlan;
import org.ncsu.settleup.settlementservice.model.Transfer;
import org.ncsu.settleup.settlementservice.repo.TransferRepository;
import org.ncsu.settleup.settlementservice.service.SettlementService;
import org.ncsu.settleup.settlementservice.client.MembershipClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Optional;

/**
 * REST controller for computing settlements and recording transfers.  The
 * endpoints here mirror those designed in Assignment 1.
 */
@RestController
public class SettlementController {
    private final SettlementService settlementService;
    private final TransferRepository transferRepository;
    private final MembershipClient membershipClient;

    public SettlementController(SettlementService settlementService,
                                TransferRepository transferRepository,
                                MembershipClient membershipClient) {
        this.settlementService = settlementService;
        this.transferRepository = transferRepository;
        this.membershipClient = membershipClient;
    }

    /**
     * Compute a settlement plan for the given group.  This endpoint
     * aggregates outstanding balances into a minimal set of transfers.
     *
     * @param request group ID and base currency
     * @return a settlement plan
     */
    @PostMapping("/settlements/compute")
    @Operation(summary = "Compute a settlement plan for a group")
    public ResponseEntity<Object> computeSettlement(@RequestBody SettlementComputeRequest request) {
        if (!membershipClient.groupExists(request.groupId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .<Object>body("Group not found");
        }
        SettlementPlan plan = settlementService.computeSettlement(request.groupId());
        return ResponseEntity.ok((Object) plan);
    }

    /**
     * Record a completed transfer.  Persist the transfer and apply it
     * to the in-memory balances to keep them up to date.
     */
    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a completed transfer between members")
    public ResponseEntity<Object> recordTransfer(@RequestBody TransferRequest request) {
        // Validate group and members exist
        if (!membershipClient.groupExists(request.groupId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .<Object>body("Group not found");
        }
        if (!membershipClient.memberExists(request.groupId(), request.fromMemberId()) ||
                !membershipClient.memberExists(request.groupId(), request.toMemberId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .<Object>body("From or To member does not exist or is not part of the group");
        }
        Transfer transfer = new Transfer();
        transfer.setGroupId(request.groupId());
        transfer.setFromMemberId(request.fromMemberId());
        transfer.setToMemberId(request.toMemberId());
        transfer.setAmount(request.amount());
        transfer.setNote(request.note());
        Transfer saved = transferRepository.save(transfer);
        applyTransferToBalances(request.groupId(), request.fromMemberId(), request.toMemberId(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body((Object) saved);
    }

    private void applyTransferToBalances(Long groupId, Long fromMemberId, Long toMemberId, BigDecimal amount) {
        // Delegate to the settlement service to update balances
        settlementService.applyTransfer(groupId, fromMemberId, toMemberId, amount);
    }

    /**
     * Retrieve a previously recorded transfer by ID.
     */
    @GetMapping("/transfers/{id}")
    @Operation(summary = "Retrieve a transfer by ID")
    public ResponseEntity<Transfer> getTransfer(@PathVariable Long id) {
        Optional<Transfer> transfer = transferRepository.findById(id);
        return transfer.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Request body for recording a transfer.
     */
    public static record TransferRequest(Long groupId,
                                         Long fromMemberId,
                                         Long toMemberId,
                                         BigDecimal amount,
                                         String note) {
    }

    /**
     * Update an existing transfer.  When a transfer is updated, the old transfer
     * effects are reversed and the new values are applied to the in-memory balances.
     */
    @PutMapping("/transfers/{id}")
    @Operation(summary = "Update an existing transfer and adjust balances")
    public ResponseEntity<Object> updateTransfer(@PathVariable Long id,
                                                 @RequestBody TransferRequest request) {
        return transferRepository.findById(id)
                .map(existing -> {
                    // Validate group and members exist
                    if (!membershipClient.groupExists(request.groupId())) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .<Object>body("Group not found");
                    }
                    if (!membershipClient.memberExists(request.groupId(), request.fromMemberId()) ||
                            !membershipClient.memberExists(request.groupId(), request.toMemberId())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .<Object>body("From or To member does not exist or is not part of the group");
                    }
                    // Reverse the old transfer
                    settlementService.applyTransfer(
                            existing.getGroupId(),
                            existing.getToMemberId(),
                            existing.getFromMemberId(),
                            existing.getAmount()
                    );
                    // Apply new values
                    existing.setGroupId(request.groupId());
                    existing.setFromMemberId(request.fromMemberId());
                    existing.setToMemberId(request.toMemberId());
                    existing.setAmount(request.amount());
                    existing.setNote(request.note());
                    Transfer saved = transferRepository.save(existing);
                    // Apply the new transfer to balances
                    settlementService.applyTransfer(
                            saved.getGroupId(),
                            saved.getFromMemberId(),
                            saved.getToMemberId(),
                            saved.getAmount()
                    );
                    return ResponseEntity.ok((Object) saved);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .<Object>body("Transfer not found"));
    }

    /**
     * Delete a transfer.  Removing a transfer reverses its effects on the in-memory balances.
     */
    @DeleteMapping("/transfers/{id}")
    @Operation(summary = "Delete a transfer and reverse its effects")
    public ResponseEntity<String> deleteTransfer(@PathVariable Long id) {
        return transferRepository.findById(id)
                .map(existing -> {
                    settlementService.applyTransfer(
                            existing.getGroupId(),
                            existing.getToMemberId(),
                            existing.getFromMemberId(),
                            existing.getAmount()
                    );
                    transferRepository.delete(existing);
                    return ResponseEntity.ok("Transfer deleted successfully");
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transfer not found"));
    }

    /**
     * List transfers for a given group.
     */
    @GetMapping("/groups/{groupId}/transfers")
    @Operation(summary = "List transfers for a group")
    public ResponseEntity<Object> getTransfersForGroup(@PathVariable Long groupId) {
        if (!membershipClient.groupExists(groupId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .<Object>body("Group not found");
        }
        List<Transfer> transfers = transferRepository.findByGroupId(groupId);
        return ResponseEntity.ok((Object) transfers);
    }
}
