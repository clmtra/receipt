package com.shop.receipt.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order{
    private Long primaryKey;
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String product;
    private String orderStatus;
    private double deliveryPrice;
    private double subTotal;
    private double totalAmount;
    private String temporaryReceiptStatus;
    private String additionalComment;
    private String finalReceiptStatusByOFD;
}
