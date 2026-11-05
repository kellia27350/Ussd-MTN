package com.example.productcatalogue.controller;

import com.example.productcatalogue.dto.TopUpRequest;
import com.example.productcatalogue.model.CustomerAccount;
import com.example.productcatalogue.repository.CustomerAccountRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/customers")
class CustomerController {

    private final CustomerAccountRepository repo;

    public CustomerController(CustomerAccountRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{phoneNumber}")
    public ResponseEntity<CustomerAccount> getAccount(@PathVariable String phoneNumber) {
        return ResponseEntity.ok(getOrCreate(phoneNumber));
    }

    @PostMapping("/{phoneNumber}/topup")
    public ResponseEntity<CustomerAccount> topUp(@PathVariable String phoneNumber,
                                                 @Valid @RequestBody TopUpRequest request) {
        CustomerAccount account = getOrCreate(phoneNumber);
        BigDecimal remaining = request.getAmount();

        if (account.getOutstandingLoan().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal repayment = remaining.min(account.getOutstandingLoan());
            account.setOutstandingLoan(account.getOutstandingLoan().subtract(repayment));
            remaining = remaining.subtract(repayment);
        }

        account.setAirtimeBalance(account.getAirtimeBalance().add(remaining));
        return ResponseEntity.ok(repo.save(account));
    }

    private CustomerAccount getOrCreate(String phoneNumber) {
        return repo.findByPhoneNumber(phoneNumber).orElseGet(() -> {
            CustomerAccount account = new CustomerAccount();
            account.setPhoneNumber(phoneNumber);
            account.setAirtimeBalance(CustomerAccount.DEFAULT_STARTING_BALANCE);
            account.setOutstandingLoan(BigDecimal.ZERO);
            return repo.save(account);
        });
    }
}
