package com.shop.receipt.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RobokassaConnect {
    public String request(String requestBody, boolean isSecondReceiptRequest) {
        String urlForSecondReceipt = "https://ws.roboxchange.com/RoboFiscal/Receipt/Attach";
        String urlForFinalStatus = "https://ws.roboxchange.com/RoboFiscal/Receipt/Status";

        try {
            URL url = new URL(isSecondReceiptRequest ? urlForSecondReceipt : urlForFinalStatus);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Length", String.valueOf(requestBody.getBytes().length));

            OutputStream outputStream = connection.getOutputStream();
            byte[] data = requestBody.getBytes(StandardCharsets.UTF_8);
            outputStream.write(data);

            connection.connect();

            return roboResponse(connection);

        } catch (IOException e) {
            e.printStackTrace();

            return "Connection fail";
        }
    }

    private String roboResponse(HttpURLConnection connection) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream inputStream = connection.getInputStream();

            byte[] buffer = new byte[inputStream.available()];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            return new String(baos.toByteArray(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();

            return "Response fail";
        }
    }

}