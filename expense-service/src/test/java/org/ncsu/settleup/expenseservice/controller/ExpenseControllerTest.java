package org.ncsu.settleup.expenseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ncsu.settleup.expenseservice.client.MembershipClient;
import org.ncsu.settleup.expenseservice.model.Expense;
import org.ncsu.settleup.expenseservice.model.SplitLine;
import org.ncsu.settleup.expenseservice.repo.ExpenseRepository;
import org.ncsu.settleup.expenseservice.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link ExpenseController}.  These tests focus on
 * cross‑service validation logic and error handling.  The
 * {@link MembershipClient} and {@link ExpenseService} are mocked
 * so that we can simulate various scenarios without hitting the
 * database or the membership service.
 */
@WebMvcTest(controllers = ExpenseController.class)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MembershipClient membershipClient;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private ExpenseRepository expenseRepository;

    @Test
    @DisplayName("createExpense returns 400 when group does not exist")
    void createExpense_invalidGroup_returnsBadRequest() throws Exception {
        // Arrange
        when(membershipClient.groupExists(999L)).thenReturn(false);
        ExpenseController.ExpenseRequest request = new ExpenseController.ExpenseRequest(
                999L,
                1L,
                "USD",
                BigDecimal.valueOf(50),
                List.of()
        );
        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Group does not exist"));
        // Verify that no expense was recorded
        verify(expenseService, never()).recordExpense(any());
    }

    @Test
    @DisplayName("createExpense returns 400 when payer is not a member of the group")
    void createExpense_invalidPayer_returnsBadRequest() throws Exception {
        // Arrange
        when(membershipClient.groupExists(1L)).thenReturn(true);
        when(membershipClient.memberExists(1L, 99L)).thenReturn(false);
        ExpenseController.ExpenseRequest request = new ExpenseController.ExpenseRequest(
                1L,
                99L,
                "USD",
                BigDecimal.valueOf(50),
                List.of()
        );
        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Payer member does not exist or is not part of the group"));
        verify(expenseService, never()).recordExpense(any());
    }

    @Test
    @DisplayName("createExpense returns 400 when a split member is not in the group")
    void createExpense_invalidSplitMember_returnsBadRequest() throws Exception {
        // Arrange
        when(membershipClient.groupExists(1L)).thenReturn(true);
        when(membershipClient.memberExists(1L, 1L)).thenReturn(true); // payer exists
        // only member 1 exists; member 2 does not
        when(membershipClient.memberExists(1L, 2L)).thenReturn(false);
        ExpenseController.SplitRequest split1 = new ExpenseController.SplitRequest(1L, BigDecimal.valueOf(30));
        ExpenseController.SplitRequest split2 = new ExpenseController.SplitRequest(2L, BigDecimal.valueOf(20));
        ExpenseController.ExpenseRequest request = new ExpenseController.ExpenseRequest(
                1L,
                1L,
                "USD",
                BigDecimal.valueOf(50),
                List.of(split1, split2)
        );
        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Split member 2 does not exist or is not part of the group"));
        verify(expenseService, never()).recordExpense(any());
    }

    @Test
    @DisplayName("createExpense returns 400 when split totals do not match the expense total")
    void createExpense_mismatchedSplits_returnsBadRequest() throws Exception {
        // Arrange
        when(membershipClient.groupExists(1L)).thenReturn(true);
        when(membershipClient.memberExists(1L, 1L)).thenReturn(true);
        when(membershipClient.memberExists(1L, 2L)).thenReturn(true);
        // Totals: 60 + 50 = 110, should not equal 100
        ExpenseController.SplitRequest split1 = new ExpenseController.SplitRequest(1L, BigDecimal.valueOf(60));
        ExpenseController.SplitRequest split2 = new ExpenseController.SplitRequest(2L, BigDecimal.valueOf(50));
        ExpenseController.ExpenseRequest request = new ExpenseController.ExpenseRequest(
                1L,
                1L,
                "USD",
                BigDecimal.valueOf(100),
                List.of(split1, split2)
        );
        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Sum of splits must equal total amount"));
        verify(expenseService, never()).recordExpense(any());
    }

    @Test
    @DisplayName("createExpense returns 201 and records the expense when input is valid")
    void createExpense_validInput_returnsCreated() throws Exception {
        // Arrange
        when(membershipClient.groupExists(1L)).thenReturn(true);
        when(membershipClient.memberExists(1L, 1L)).thenReturn(true);
        when(membershipClient.memberExists(1L, 2L)).thenReturn(true);
        // Prepare a fake saved expense
        Expense savedExpense = new Expense();
        savedExpense.setId(123L);
        savedExpense.setGroupId(1L);
        savedExpense.setPayerMemberId(1L);
        savedExpense.setCurrency("USD");
        savedExpense.setTotalAmount(BigDecimal.valueOf(100));
        // Add splits
        List<SplitLine> splits = new ArrayList<>();
        SplitLine s1 = new SplitLine();
        s1.setMemberId(1L);
        s1.setShareAmount(BigDecimal.valueOf(60));
        s1.setExpense(savedExpense);
        SplitLine s2 = new SplitLine();
        s2.setMemberId(2L);
        s2.setShareAmount(BigDecimal.valueOf(40));
        s2.setExpense(savedExpense);
        splits.add(s1);
        splits.add(s2);
        savedExpense.setSplits(splits);
        when(expenseService.recordExpense(any(Expense.class))).thenReturn(savedExpense);

        ExpenseController.SplitRequest split1 = new ExpenseController.SplitRequest(1L, BigDecimal.valueOf(60));
        ExpenseController.SplitRequest split2 = new ExpenseController.SplitRequest(2L, BigDecimal.valueOf(40));
        ExpenseController.ExpenseRequest request = new ExpenseController.ExpenseRequest(
                1L,
                1L,
                "USD",
                BigDecimal.valueOf(100),
                List.of(split1, split2)
        );
        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(savedExpense)));
        verify(expenseService, times(1)).recordExpense(any(Expense.class));
    }
}