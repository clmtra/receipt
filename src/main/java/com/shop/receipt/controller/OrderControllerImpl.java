package com.shop.receipt.controller;

import com.shop.receipt.model.Order;
import com.shop.receipt.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.progressify.spring.annotations.CacheFirst;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderControllerImpl implements OrderController {

    private final OrderService orderService;

    @Override
    @CacheFirst
    @GetMapping
    public String searchOrderPage() {
        return "index";
    }

    @Override
    @GetMapping("/search")
    public String orderPage(@RequestParam(value = "orderId") Long id,
                            @RequestParam(value = "orderEmail") String email,
                            Model model) {
        if (email.isEmpty()) {
            List<Order> orderList = new ArrayList<>();
            orderList.add(orderService.getOrderById(id));
            model.addAttribute("orders", orderList.contains(null) ? null : orderList);
        } else {
            model.addAttribute("orders", orderService.getOrdersByEmail(email));
        }
        return "order";
    }

    @Override
    @PatchMapping("/update")
    public String updateOrderReceiptStatus(@RequestParam(value = "orderId") Long id) {
        orderService.updateOrderById(id);
        return "redirect:/order/" + id + "/result";
    }

    @Override
    @GetMapping("/{id}/result")
    public String updatedOrderPage(@PathVariable("id") Long id, Model model) {
        List<Order> orderList = new ArrayList<>();
        orderList.add(orderService.getOrderById(id));
        model.addAttribute("orders", orderList);
        return "order";
    }


}
