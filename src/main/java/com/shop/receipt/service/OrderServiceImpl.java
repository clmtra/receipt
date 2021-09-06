package com.shop.receipt.service;

import com.shop.receipt.repository.OrderRepository;
import com.shop.receipt.request.RobokassaEncoder;
import com.shop.receipt.model.Order;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private RobokassaEncoder encoder;
    private final String successTemporaryStatus = "Успешно";
    private final String errorTemporaryStatus = "Ошибка. Чек не отправлен!";
    private final String successFinalStatus = "Чек зарегистрирован";
    private final String errorFinalStatus = "Ошибка регистрации чека";
    private final String waitFinalStatus = "Ожидание регистрации чека";
    private static final Logger logger = Logger.getLogger(OrderServiceImpl.class.getName());

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.getOrderById(id);
    }

    @Override
    public List<Order> getOrdersByEmail(String email) {
        return orderRepository.getOrdersByEmail(email);
    }

    @Override
    public List<Order> getBoxberryOrders() {
        return orderRepository.getOrdersWithBoxberryDelivery();
    }

    @Override
    public List<Order> getOrdersWithSentSecondReceipt() {
        return orderRepository.getOrdersWithSentReceiptsOnly();
    }

    @Override
    public void checkFinalOFDStatus(Order order) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("Europe/Moscow"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        encoder = new RobokassaEncoder(order);
        String response = encoder.getFinalStatusOfReceipt();
        logger.info(OrderServiceImpl.class.getSimpleName() + " Robokassa Response body for Order" + " "
                + order.getId() + ": " + response);

        String responseStatus = getResponseStatus(response);
        if (responseStatus != null && !order.getFinalReceiptStatusByOFD().startsWith(successFinalStatus)
                && !order.getFinalReceiptStatusByOFD().startsWith(responseStatus)) {
            order.setFinalReceiptStatusByOFD(responseStatus + ": " + dateFormat.format(calendar.getTime()));
            statusActualizer(order, responseStatus);
            logger.info(OrderServiceImpl.class.getSimpleName() + " finalReceiptStatus CHANGED FOR: " + order);

            orderRepository.updateOfdStatus(order);
            logger.info(OrderServiceImpl.class.getSimpleName() + " " + order.getId() + " order updated");
        }
    }

    @Override
    public synchronized void updateOrderById(Long id) {
        Order order = getOrderById(id);
        encoder = new RobokassaEncoder(order);
        String response = encoder.sendEncodedSecondReceipt();
        logger.info(OrderServiceImpl.class.getSimpleName() + " - Response for " + id.toString() + ": " + response);

        try {
            String resultCode = new JSONObject(response).get("ResultCode").toString();

            if (resultCode.equals("0")) {
                orderRepository.updateOrderStatus(order, successTemporaryStatus, resultCode);
            } else {
                orderRepository.updateOrderStatus(order, errorTemporaryStatus, resultCode);
            }
        } catch (Exception e) {
            logger.warn("ORDER SERVICE UPD: DELETE COMMENT SLASH FROM ENCODER");
            e.printStackTrace();
        }
    }

    private void statusActualizer(Order order, String responseStatus) {
        if (!isCorrectTemporaryStatus(order)) {
            orderRepository.updateReceiptStatus(order, successTemporaryStatus);
            logger.info(OrderServiceImpl.class.getSimpleName() + " TEMPORARY STATUS WAS CHANGED FOR: " + order.getId());
        } else if (responseStatus.equals(errorFinalStatus) || responseStatus.equals(waitFinalStatus) &&
                !order.getTemporaryReceiptStatus().equals(errorTemporaryStatus)) {
            orderRepository.updateReceiptStatus(order, errorTemporaryStatus);
            logger.error(OrderServiceImpl.class.getSimpleName() + " ERROR STATUS: " + responseStatus + " by " + order);
        }
    }

    private boolean isCorrectTemporaryStatus(Order order) {
        if (order.getOrderStatus().equals("Да") && !order.getTemporaryReceiptStatus().equals(successTemporaryStatus)
                && order.getFinalReceiptStatusByOFD().startsWith(successFinalStatus)) {
            return false;
        } else {
            return true;
        }
    }

    private String getResponseStatus(String response) {
        String responseStatus = null;
        try {
            JSONArray receiptsArray = new JSONObject(response).getJSONArray("Statuses");
            String statusOfSecondReceipt = receiptsArray.getJSONObject(1).get("Code").toString();
            switch (statusOfSecondReceipt) {
                case "1":
                    responseStatus = waitFinalStatus;
                    break;
                case "2":
                    responseStatus = successFinalStatus;
                    break;
                case "3":
                    responseStatus = errorFinalStatus;
                    break;
                case "0":
                    responseStatus = "Второй чек не выдавался";
            }
        } catch (Exception e) {
            if (response != null && new JSONObject(response).get("Code").toString().equals("1000")) {
                responseStatus = "Ошибка обработки запроса";
                logger.error("Check " + response);
            }
        }
        return responseStatus;
    }
}
