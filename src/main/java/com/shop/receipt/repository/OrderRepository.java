package com.shop.receipt.repository;

import com.shop.receipt.model.Order;

import java.util.List;


public interface OrderRepository {

    Order getOrderById(Long id);

    List<Order> getOrdersByEmail(String name);

    void updateOrderStatus(Order order, String receiptStatus, String statusCode);

    List<Order> getOrdersWithBoxberryDelivery();

    List<Order> getOrdersWithSentReceiptsOnly();

    void updateOfdStatus(Order order);

    void updateReceiptStatus(Order order, String receiptStatus);
}
