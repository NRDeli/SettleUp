package org.ncsu.settleup.expenseservice.service;

import org.ncsu.settleup.common.events.ExpenseRecordedEvent;
import org.ncsu.settleup.expenseservice.config.AmqpConfig;
import org.ncsu.settleup.expenseservice.model.Expense;
import org.ncsu.settleup.expenseservice.model.SplitLine;
import org.ncsu.settleup.expenseservice.repo.ExpenseRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Business service for handling expense persistence and event publication.
 */
@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final RabbitTemplate rabbitTemplate;

    public ExpenseService(ExpenseRepository expenseRepository,
                          RabbitTemplate rabbitTemplate) {
        this.expenseRepository = expenseRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Persist an expense and publish an {@link ExpenseRecordedEvent} so that
     * other services can update their state.  The event includes a map
     * from member IDs to the amount they owe for the expense.
     *
     * @param expense the expense to persist
     * @return the saved expense
     */
    public Expense recordExpense(Expense expense) {
        // Persist the expense along with its split lines
        Expense saved = expenseRepository.save(expense);

        // Build a map of member IDs to share amounts for the event
        Map<Long, BigDecimal> shares = new HashMap<>();
        for (SplitLine split : saved.getSplits()) {
            shares.put(split.getMemberId(), split.getShareAmount());
        }

        // Publish an event indicating a new expense has been recorded
        ExpenseRecordedEvent event = new ExpenseRecordedEvent(
                saved.getGroupId(),
                saved.getId(),
                saved.getPayerMemberId(),
                shares);
        rabbitTemplate.convertAndSend(AmqpConfig.EXCHANGE_NAME, "expense.recorded", event);

        return saved;
    }
}
