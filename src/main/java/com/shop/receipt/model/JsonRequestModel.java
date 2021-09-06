package com.shop.receipt.model;

import com.shop.receipt.request.ItemsParserUtil;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

@RequiredArgsConstructor
public class JsonRequestModel {

    private final Order order;
    private final String merchantId = System.getProperty("merchant");
    private final String url = System.getProperty("url");

    public JSONObject secondReceipt() {
        JSONObject requestBody = new JSONObject();

        requestBody.put("merchantId", merchantId);
        requestBody.put("id", "1" + order.getId()); // 1 + paymentId
        requestBody.put("originId", String.valueOf(order.getId())); // paymentId;
        requestBody.put("operation", "sell");
        requestBody.put("url", url);
        requestBody.put("total", order.getTotalAmount());
        requestBody.put("items", allItems());
        requestBody.put("client", client());
        requestBody.put("payments", payments());
        requestBody.put("vats", vats());

        return requestBody;
    }

    public JSONObject statusOfFinalReceipt() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("merchantId", merchantId);
        requestBody.put("id", String.valueOf(order.getId()));

        return requestBody;
    }

    private JSONObject client() {
        JSONObject clientJsonObj = new JSONObject();
        if (order.getEmail() != null && !order.getEmail().isEmpty()) {
            clientJsonObj.put("email", order.getEmail());
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < order.getPhoneNumber().length(); i++) {
                if (Character.isDigit(order.getPhoneNumber().charAt(i))) {
                    builder.append(order.getPhoneNumber().charAt(i));
                }
            }
            clientJsonObj.put("phone", builder.toString());
        }

        return clientJsonObj;
    }

    private JSONArray vats() {
        JSONObject vatsJsonObj = new JSONObject();
        vatsJsonObj.put("type", "none");
        vatsJsonObj.put("sum", 0);

        JSONArray vatsArray = new JSONArray();
        vatsArray.put(vatsJsonObj);

        return vatsArray;
    }

    private JSONArray payments() {
        JSONObject paymentsJsonObj = new JSONObject();
        paymentsJsonObj.put("type", 2);
        paymentsJsonObj.put("sum", order.getTotalAmount());

        JSONArray paymentsArray = new JSONArray();
        paymentsArray.put(paymentsJsonObj);

        return paymentsArray;
    }

    private JSONArray allItems() {
        List<List<String>> productsList = new ItemsParserUtil(order).getItems();

        JSONArray itemsArray = new JSONArray();
        for (List<String> strings : productsList) {
            itemsArray.put(item(strings));
        }

        if (order.getDeliveryPrice() != 0) {
            JSONObject boxberry = new JSONObject();
            boxberry.put("name", "Boxberry");
            boxberry.put("quantity", 1);
            boxberry.put("sum", order.getDeliveryPrice());
            boxberry.put("tax", "none");

            itemsArray.put(boxberry);
        }

        return itemsArray;
    }

    private JSONObject item(List<String> onceItem) {
        JSONObject itemPositions = new JSONObject();

        itemPositions.put("name", onceItem.get(0));
        itemPositions.put("quantity", Integer.parseInt(onceItem.get(1)));
        itemPositions.put("sum", Double.parseDouble(onceItem.get(2)));
        itemPositions.put("tax", onceItem.get(3));

        return itemPositions;
    }

}




