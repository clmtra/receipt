package com.shop.receipt.controller;

import org.springframework.ui.Model;

public interface OrderController {

    String searchOrderPage();

    String orderPage(Long id, String email, Model model);

    String updateOrderReceiptStatus(Long id);

    String updatedOrderPage(Long id, Model model);
}
