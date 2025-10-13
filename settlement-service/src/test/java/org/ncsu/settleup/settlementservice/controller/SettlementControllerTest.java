package org.ncsu.settleup.settlementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ncsu.settleup.common.dto.SettlementComputeRequest;
import org.ncsu.settleup.settlementservice.client.MembershipClient;
import org.ncsu.settleup.settlementservice.model.Transfer;
import org.ncsu.settleup.settlementservice.repo.TransferRepository;
import org.ncsu.settleup.settlementservice.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link SettlementController}.  These tests exercise
 * cross‑service validation and error handling for computing
 * settlements and recording transfers.  External dependencies such
 * as {@link MembershipClient}, {@link TransferRepository} and
 * {@link SettlementService} are mocked to ensure the controller
 * behaviour is isolated.
 */
@WebMvcTest(controllers = SettlementController.class)
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MembershipClient membershipClient;

    @MockBean
    private TransferRepository transferRepository;

    @MockBean
    private SettlementService settlementService;

    @Test
    @DisplayName("computeSettlement returns 404 when group does not exist")
    void computeSettlement_invalidGroup_returnsNotFound() throws Exception {
        // Arrange
        when(membershipClient.groupExists(999L)).thenReturn(false);
        SettlementComputeRequest request = new SettlementComputeRequest(999L, "USD");
        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/settlements/compute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Group not found"));
    }

    @Test
    @DisplayName("recordTransfer returns 404 when group does not exist")
    void recordTransfer_invalidGroup_returnsNotFound() throws Exception {
        // Arrange
        when(membershipClient.groupExists(999L)).thenReturn(false);
        SettlementController.TransferRequest request = new SettlementController.TransferRequest(999L, 1L, 2L, BigDecimal.TEN, "note");
        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Group not found"));
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordTransfer returns 400 when a member is not in the group")
    void recordTransfer_invalidMembers_returnsBadRequest() throws Exception {
        // Arrange
        when(membershipClient.groupExists(1L)).thenReturn(true);
        // Simulate that at least one of the members does not belong to the group
        when(membershipClient.memberExists(1L, 1L)).thenReturn(true);
        when(membershipClient.memberExists(1L, 2L)).thenReturn(false);
        SettlementController.TransferRequest request = new SettlementController.TransferRequest(1L, 1L, 2L, BigDecimal.TEN, "note");
        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("From or To member does not exist or is not part of the group"));
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordTransfer returns 201 and saves transfer when input is valid")
    void recordTransfer_validInput_returnsCreated() throws Exception {
        // Arrange
        when(membershipClient.groupExists(1L)).thenReturn(true);
        when(membershipClient.memberExists(1L, 1L)).thenReturn(true);
        when(membershipClient.memberExists(1L, 2L)).thenReturn(true);
        // Prepare saved transfer
        Transfer saved = new Transfer();
        saved.setId(10L);
        saved.setGroupId(1L);
        saved.setFromMemberId(1L);
        saved.setToMemberId(2L);
        saved.setAmount(BigDecimal.TEN);
        saved.setNote("note");
        when(transferRepository.save(any(Transfer.class))).thenReturn(saved);
        // applyTransfer should be called once to update balances
        doNothing().when(settlementService).applyTransfer(1L, 1L, 2L, BigDecimal.TEN);
        SettlementController.TransferRequest request = new SettlementController.TransferRequest(1L, 1L, 2L, BigDecimal.TEN, "note");
        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(saved)));
        verify(transferRepository, times(1)).save(any(Transfer.class));
        verify(settlementService, times(1)).applyTransfer(1L, 1L, 2L, BigDecimal.TEN);
    }
}