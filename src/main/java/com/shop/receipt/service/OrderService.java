package com.shop.receipt.service;

import com.shop.receipt.model.Order;

import java.util.List;

public interface OrderService {
    Order getOrderById(Long id);

    List<Order> getOrdersByEmail(String email);

    List<Order> getBoxberryOrders();

    List<Order> getOrdersWithSentSecondReceipt();

    void checkFinalOFDStatus(Order order);

    void updateOrderById(Long id);
}
