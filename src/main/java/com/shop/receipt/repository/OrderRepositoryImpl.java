package com.shop.receipt.repository;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.shop.receipt.auth.SheetsAuth;
import com.shop.receipt.model.Order;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class OrderRepositoryImpl implements OrderRepository {
    private Sheets sheetsService;
    private static final Logger logger = Logger.getLogger(OrderRepositoryImpl.class.getName());

    @Override
    public Order getOrderById(Long id) {
        List<List<Object>> allSheet = getSheet();

        if (allSheet == null || allSheet.isEmpty()) {
            return null;
        }

        for (int i = 1; i < allSheet.size(); i++) {
            if (allSheet.get(i).get(3).equals(String.valueOf(id))) {
                try {
                    return getOrder((long) i + 1, allSheet.get(i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public List<Order> getOrdersByEmail(String email) {
        List<Order> resultOrders = new ArrayList<>();
        List<List<Object>> allSheet = getSheet();


        if (allSheet == null || allSheet.isEmpty()) {
            logger.info(OrderRepositoryImpl.class.getSimpleName() + " all sheet is empty");
            return null;
        }

        for (int i = 1; i < allSheet.size(); i++) {
            try {
                if (allSheet.get(i).get(1).toString().toLowerCase().trim().contains(email.toLowerCase())) {
                    resultOrders.add(getOrder((long) i + 1, allSheet.get(i)));
                }
            } catch (Exception e){
                e.printStackTrace();
            }


        }

        return resultOrders.size() < 1 ? null : resultOrders;
    }

    @Override
    public List<Order> getOrdersWithBoxberryDelivery() {
        String deliveryMethod = "boxberry";
        List<Order> resultOrders = new ArrayList<>();
        List<List<Object>> allSheet = getSheet();

        if (allSheet == null || allSheet.isEmpty()) return null;

        for (int i = 1; i < allSheet.size(); i++) {
            if (allSheet.get(i).get(0).toString().equalsIgnoreCase("Нет") ||
                    allSheet.get(i).get(0).toString().isEmpty() &&
                            allSheet.get(i).get(7).toString().toLowerCase().contains(deliveryMethod)) {
                try {
                    resultOrders.add(getOrder((long) i + 1, allSheet.get(i)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return resultOrders;
    }

    @Override
    public void updateOrderStatus(Order order, String receiptStatus, String resultCode) {
        if (order != null && order.getOrderStatus().equals("Нет")) {
            String positionForUpdate = "A" + order.getPrimaryKey();
            ValueRange body = new ValueRange()
                    .setValues(Arrays.asList(
                            Arrays.asList("Да")
                    ));
            try {
                getSheetsService().spreadsheets().values()
                        .update(SheetsAuth.getSpreadsheetId(), positionForUpdate, body)
                        .setValueInputOption("RAW")
                        .execute();

                updateReceiptStatus(order, receiptStatus);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (order != null && order.getOrderStatus().equals("Да") || order != null && resultCode.equals("1000")) {
            updateReceiptStatus(order, "Успешно");
        }
    }

    @Override
    public List<Order> getOrdersWithSentReceiptsOnly() {
        List<List<Object>> allSheet = getSheet();
        List<Order> resultOrders = new ArrayList<>();

        if (allSheet.isEmpty()) return null;

        long orderPrimaryKey = 1;
        for (List<Object> row : getSheet()) {
            if (row.get(0).toString().equalsIgnoreCase("Да") &&
                    !row.get(14).toString().startsWith("Чек зарегистрирован")) {
                try {
                    resultOrders.add(getOrder(orderPrimaryKey, row));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            orderPrimaryKey++;
        }

        return resultOrders;
    }

    @Override
    public void updateOfdStatus(Order order) {
        String positionForUpdate = "O" + order.getPrimaryKey();
        ValueRange body = new ValueRange()
                .setValues(Arrays.asList(
                        Arrays.asList(order.getFinalReceiptStatusByOFD())
                ));
        try {
            getSheetsService().spreadsheets().values().
                    update(SheetsAuth.getSpreadsheetId(), positionForUpdate, body)
                    .setValueInputOption("RAW")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void updateReceiptStatus(Order order, String receiptStatus) {
        if (order != null) {
            String positionForUpdate = "F" + order.getPrimaryKey();
            ValueRange body = new ValueRange()
                    .setValues(Arrays.asList(
                            Arrays.asList(receiptStatus)
                    ));
            try {
                getSheetsService().spreadsheets().values().
                        update(SheetsAuth.getSpreadsheetId(), positionForUpdate, body)
                        .setValueInputOption("RAW")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Order getOrder(Long primaryKey, List<Object> row) {
        String paymentId = row.get(3).toString();
        String name = row.get(6).toString();
        String email = row.get(1).toString();
        String phone = row.get(2).toString();
        String product = row.get(4).toString().replaceAll("&quot;", "\"");
        String subtotal = parseSubTotal(product);
        String orderStatus = row.get(0).toString();
        String deliveryPrice = row.get(8).toString();
        String totalAmount = row.get(15).toString();
        String receiptStatus = row.get(5).toString();
        String additionalComment = row.get(13).toString().trim().startsWith("https://shop") ?
                "Нет" : row.get(13).toString().trim();
        String finalReceiptStatusByOFD = row.get(14).toString().trim();

        return new Order(
                primaryKey, // row number in google sheets
                paymentId.isEmpty() ? 0 : Long.parseLong(paymentId), // id
                name, // name
                email, // email
                phone, // phone
                product, // product
                orderStatus.isEmpty() ? "Нет" : orderStatus, // order status
                deliveryPrice.isEmpty() ? 0 : Double.parseDouble(deliveryPrice), // delivery price
                Double.parseDouble(subtotal), // subtotal
                totalAmount.isEmpty() ? 0 : Double.parseDouble(totalAmount), // total amount
                receiptStatus.equals("RUB") ? "Нет" : receiptStatus, // receipt status
                additionalComment.isEmpty() ? "Нет" : additionalComment,
                finalReceiptStatusByOFD.isEmpty() ? "Нет" : finalReceiptStatusByOFD);
    }

    private String parseSubTotal(String product) {
        int productsSum = 0;
        if (product.contains(";")) {
            String[] positionsArray = product.split(";");
            String sumOfProduct;
            for (int i = 0; i < positionsArray.length; i++) {
                sumOfProduct = positionsArray[i];
                sumOfProduct = sumOfProduct.substring(sumOfProduct.indexOf("≡") + 1).trim();
                productsSum += Integer.parseInt(sumOfProduct);
            }
        } else if (product.contains("≡")) {
            product = product.substring(product.indexOf("≡") + 1).trim();
            productsSum = Integer.parseInt(product);
        }

        return String.valueOf(productsSum);
    }

    private Sheets getSheetsService() {
        try {
            sheetsService = SheetsAuth.getSheetsService();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }

        return sheetsService;
    }

    private List<List<Object>> getSheet() {
        ValueRange response = null;

        try {
            String range = "LeadsFromTilda";
            response = getSheetsService().spreadsheets().values()
                    .get(SheetsAuth.getSpreadsheetId(), range)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response.getValues();
    }
}

