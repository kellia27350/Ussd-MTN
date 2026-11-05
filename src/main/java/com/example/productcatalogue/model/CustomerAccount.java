package com.example.productcatalogue.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_accounts")
public class CustomerAccount {

    public static final BigDecimal DEFAULT_STARTING_BALANCE = new BigDecimal("1000.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal airtimeBalance = DEFAULT_STARTING_BALANCE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal outstandingLoan = BigDecimal.ZERO;

    public CustomerAccount() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public BigDecimal getAirtimeBalance() { return airtimeBalance; }
    public void setAirtimeBalance(BigDecimal airtimeBalance) { this.airtimeBalance = airtimeBalance; }
    public BigDecimal getOutstandingLoan() { return outstandingLoan; }
    public void setOutstandingLoan(BigDecimal outstandingLoan) { this.outstandingLoan = outstandingLoan; }
}
