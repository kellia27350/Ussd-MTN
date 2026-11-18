package com.example.productcatalogue.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row for every product the caller opens or views during a USSD session.
 * Product/category details are stored as a snapshot (no foreign key), so the
 * history stays correct even if the product is later renamed, re-priced or deleted.
 */
@Entity
@Table(name = "ussd_product_selections", indexes = {
        @Index(name = "idx_sel_session", columnList = "sessionId"),
        @Index(name = "idx_sel_phone", columnList = "phoneNumber"),
        @Index(name = "idx_sel_product", columnList = "productId")
})
public class UssdProductSelection {

    /** Caller opened a bundle group and was shown its sub-products. */
    public static final String TYPE_GROUP_OPENED = "GROUP_OPENED";
    /** Caller reached the final product detail screen. */
    public static final String TYPE_DETAIL_VIEWED = "DETAIL_VIEWED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    private String selectionType;

    private Long productId;
    private String productName;
    private Long parentProductId;
    private Long categoryId;
    private String categoryName;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    private Integer validityDays;

    @Column(length = 10)
    private String language;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UssdProductSelection() {}

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String v) { this.phoneNumber = v; }
    public String getSelectionType() { return selectionType; }
    public void setSelectionType(String v) { this.selectionType = v; }
    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public Long getParentProductId() { return parentProductId; }
    public void setParentProductId(Long v) { this.parentProductId = v; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long v) { this.categoryId = v; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String v) { this.categoryName = v; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { this.price = v; }
    public Integer getValidityDays() { return validityDays; }
    public void setValidityDays(Integer v) { this.validityDays = v; }
    public String getLanguage() { return language; }
    public void setLanguage(String v) { this.language = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
