package org.ncsu.settleup.expenseservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an expense paid by a member on behalf of a group.  The
 * expense maintains a collection of splits indicating how the total
 * amount is apportioned among members.
 */
@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the group this expense belongs to. */
    private Long groupId;

    /** ID of the member who paid the expense. */
    private Long payerMemberId;

    private String currency;

    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SplitLine> splits = new ArrayList<>();

    public Expense() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getPayerMemberId() {
        return payerMemberId;
    }

    public void setPayerMemberId(Long payerMemberId) {
        this.payerMemberId = payerMemberId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<SplitLine> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitLine> splits) {
        this.splits = splits;
    }
}
