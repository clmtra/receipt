package com.shop.receipt.request;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BoxberryStatusChecker {

    public boolean getBoxberryStatus(String orderId) {

        String response = boxberryConnection(orderId);
        boolean result = !response.startsWith("ERROR") && isDelivered(response);
        if (result) {
            System.out.println("Order: " + orderId + " " + response);
        }

        return result;
    }

    private String boxberryConnection(String id) {
        String urlWithToken = "https://api.boxberry.ru/json.php?method=ListStatuses&token=61fc1d099a24015e6217183e9&ImId=";
        HttpURLConnection connection;

        try {
            URL url = new URL(urlWithToken + id);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            return connection.getResponseCode() == 200 ?
                    responseBody(connection.getInputStream()) :
                    "ERROR " + connection.getResponseCode() + ": " + responseBody(connection.getErrorStream());
        } catch (IOException e) {
            return "boxberryConnection // Wrong url or invalid url connection: " + e;
        }
    }

    private String responseBody(InputStream inputStream) {
        StringBuilder content;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String inputLine;
            content = new StringBuilder();
            while ((inputLine = bufferedReader.readLine()) != null) {
                content.append(inputLine);
            }
        } catch (IOException e) {
            content = new StringBuilder(this.getClass().getSimpleName() + "Unknown exception");
        }

        return content.toString();
    }

    private boolean isDelivered(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            for (Object obj : jsonArray) {
                if (new JSONObject(obj.toString()).get("Name").equals("Выдано")) {
                    return true;
                }
            }
        } catch (Exception e) {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.has("Name") && jsonObject.get("Name").equals("Выдано")) {
                return true;
            }
        }
        return false;
    }
}
