package org.ncsu.settleup.expenseservice.controller;

import org.ncsu.settleup.expenseservice.model.Expense;
import org.ncsu.settleup.expenseservice.model.SplitLine;
import org.ncsu.settleup.expenseservice.repo.ExpenseRepository;
import org.ncsu.settleup.expenseservice.service.ExpenseService;
import org.ncsu.settleup.expenseservice.client.MembershipClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for recording and retrieving expenses.  The API
 * corresponds to the design from Assignment 1.
 */
@RestController
public class ExpenseController {
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final MembershipClient membershipClient;

    public ExpenseController(ExpenseService expenseService,
                             ExpenseRepository expenseRepository,
                             MembershipClient membershipClient) {
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
        this.membershipClient = membershipClient;
    }

    /**
     * Record a new expense.  The payer is identified by payerMemberId and
     * the expense is apportioned among members according to the splits.
     *
     * @param request request body describing the expense
     * @return the persisted expense
     */
    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a new expense with splits")
    public Expense createExpense(@RequestBody ExpenseRequest request) {
        // Validate that the group exists
        if (!membershipClient.groupExists(request.groupId())) {
            throw new IllegalArgumentException("Group does not exist");
        }
        // Validate payer exists in the group
        if (!membershipClient.memberExists(request.groupId(), request.payerMemberId())) {
            throw new IllegalArgumentException("Payer member does not exist or is not part of the group");
        }
        // Validate splits and that each member exists
        BigDecimal sum = BigDecimal.ZERO;
        List<SplitLine> splitLines = new ArrayList<>();
        if (request.splits() != null) {
            for (SplitRequest splitReq : request.splits()) {
                if (!membershipClient.memberExists(request.groupId(), splitReq.memberId())) {
                    throw new IllegalArgumentException("Split member " + splitReq.memberId() + " does not exist or is not part of the group");
                }
                SplitLine split = new SplitLine();
                split.setMemberId(splitReq.memberId());
                split.setShareAmount(splitReq.shareAmount());
                splitLines.add(split);
                sum = sum.add(splitReq.shareAmount());
            }
        }
        if (request.totalAmount() != null && sum.compareTo(request.totalAmount()) != 0) {
            throw new IllegalArgumentException("Sum of splits must equal total amount");
        }
        Expense expense = new Expense();
        expense.setGroupId(request.groupId());
        expense.setPayerMemberId(request.payerMemberId());
        expense.setCurrency(request.currency());
        expense.setTotalAmount(request.totalAmount());
        // Associate splits with expense
        for (SplitLine sl : splitLines) {
            sl.setExpense(expense);
        }
        expense.setSplits(splitLines);
        return expenseService.recordExpense(expense);
    }

    /**
     * Retrieve an expense by ID.
     *
     * @param id expense identifier
     * @return the expense if found, 404 otherwise
     */
    @GetMapping("/expenses/{id}")
    @Operation(summary = "Retrieve an expense by ID")
    public ResponseEntity<Expense> getExpense(@PathVariable Long id) {
        return expenseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Request payload for creating an expense.
     */
    public static record ExpenseRequest(Long groupId,
                                        Long payerMemberId,
                                        String currency,
                                        BigDecimal totalAmount,
                                        List<SplitRequest> splits) {
    }

    /**
     * Request payload for a split line.
     */
    public static record SplitRequest(Long memberId, BigDecimal shareAmount) {
    }

    /**
     * Update an existing expense.  All fields are replaced.  Splits must be provided in full.
     *
     * @param id expense identifier
     * @param request updated expense data
     * @return the updated expense
     */
    @PutMapping("/expenses/{id}")
    @Operation(summary = "Update an existing expense with new values and splits")
    public ResponseEntity<Object> updateExpense(@PathVariable Long id,
                                           @RequestBody ExpenseRequest request) {
        return expenseRepository.findById(id)
                .map(existing -> {
                    ResponseEntity<Object> validationError = validateGroupAndPayer(request);
                    if (validationError != null) {
                        return validationError;
                    }

                    SplitValidationResult splitValidation = validateSplits(existing, request);
                    if (splitValidation.hasError()) {
                        return splitValidation.errorResponse();
                    }

                    BigDecimal sum = splitValidation.sum();
                    List<SplitLine> newSplits = splitValidation.splits();

                    if (request.totalAmount() != null && sum.compareTo(request.totalAmount()) != 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .<Object>body("Sum of splits must equal total amount");
                    }

                    existing.setGroupId(request.groupId());
                    existing.setPayerMemberId(request.payerMemberId());
                    existing.setCurrency(request.currency());
                    existing.setTotalAmount(request.totalAmount());
                    // Remove old splits and set new
                    existing.getSplits().clear();
                    existing.setSplits(newSplits);
                    Expense saved = expenseRepository.save(existing);
                    return ResponseEntity.ok((Object) saved);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).<Object>body("Expense not found"));
    }

    private ResponseEntity<Object> validateGroupAndPayer(ExpenseRequest request) {
        if (!membershipClient.groupExists(request.groupId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .<Object>body("Group does not exist");
        }
        if (!membershipClient.memberExists(request.groupId(), request.payerMemberId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .<Object>body("Payer does not exist or is not part of the group");
        }
        return null;
    }

    private SplitValidationResult validateSplits(Expense existing, ExpenseRequest request) {
        BigDecimal sum = BigDecimal.ZERO;
        List<SplitLine> newSplits = new ArrayList<>();
        if (request.splits() != null) {
            for (SplitRequest sr : request.splits()) {
                if (!membershipClient.memberExists(request.groupId(), sr.memberId())) {
                    ResponseEntity<Object> errorResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .<Object>body("Split member " + sr.memberId() + " does not exist or is not part of the group");
                    return SplitValidationResult.error(errorResponse);
                }
                SplitLine sl = new SplitLine();
                sl.setMemberId(sr.memberId());
                sl.setShareAmount(sr.shareAmount());
                sl.setExpense(existing);
                newSplits.add(sl);
                sum = sum.add(sr.shareAmount());
            }
        }
        return SplitValidationResult.success(newSplits, sum);
    }

    private static class SplitValidationResult {
        private final List<SplitLine> splits;
        private final BigDecimal sum;
        private final ResponseEntity<Object> error;

        private SplitValidationResult(List<SplitLine> splits, BigDecimal sum, ResponseEntity<Object> error) {
            this.splits = splits;
            this.sum = sum;
            this.error = error;
        }

        static SplitValidationResult success(List<SplitLine> splits, BigDecimal sum) {
            return new SplitValidationResult(splits, sum, null);
        }

        static SplitValidationResult error(ResponseEntity<Object> error) {
            return new SplitValidationResult(null, null, error);
        }

        boolean hasError() {
            return error != null;
        }

        ResponseEntity<Object> errorResponse() {
            return error;
        }

        List<SplitLine> splits() {
            return splits;
        }

        BigDecimal sum() {
            return sum;
        }
    }

    /**
     * Delete an expense.
     */
    @DeleteMapping("/expenses/{id}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<String> deleteExpense(@PathVariable Long id) {
        return expenseRepository.findById(id)
                .map(expense -> {
                    expenseRepository.delete(expense);
                    return ResponseEntity.ok("Expense deleted successfully");
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found"));
    }

    /**
     * List all expenses for a given group.
     */
    @GetMapping("/groups/{groupId}/expenses")
    @Operation(summary = "List expenses for a given group")
    public ResponseEntity<Object> getExpensesForGroup(@PathVariable Long groupId) {
        // Validate group exists
        if (!membershipClient.groupExists(groupId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .<Object>body("Group not found");
        }
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        return ResponseEntity.ok((Object) expenses);
    }
}
