package com.shop.receipt.request;

import com.shop.receipt.model.JsonRequestModel;
import com.shop.receipt.model.Order;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RequiredArgsConstructor
public class RobokassaEncoder {
    private final Order order;
    private static final Logger logger = Logger.getLogger(RobokassaEncoder.class.getName());

    public String sendEncodedSecondReceipt() {
        JsonRequestModel jsonModel = new JsonRequestModel(order);
        String body = jsonModel.secondReceipt().toString();
        logger.info(RobokassaEncoder.class.getSimpleName() + " - Request body for " + order.getId().toString() + ": "
                + body);

        String encodedRequestBody = Base64.getUrlEncoder().withoutPadding().encodeToString(
                body.getBytes(StandardCharsets.UTF_8)
        );

        return new RobokassaConnect().request(buildRequestByRobokassaRules(encodedRequestBody), true);

    }

    public String getFinalStatusOfReceipt() {
        JsonRequestModel jsonModel = new JsonRequestModel(order);
        String encodedRequestBody = Base64.getUrlEncoder().withoutPadding().encodeToString(
                jsonModel.statusOfFinalReceipt().toString().getBytes(StandardCharsets.UTF_8)
        );

        return new RobokassaConnect()
                .request(buildRequestByRobokassaRules(encodedRequestBody), false);
    }

    private String buildRequestByRobokassaRules(String encodedRequestBody) {
        String merchantPass = System.getProperty("pass");
        String signature = encodedRequestBody.concat(merchantPass);
        String mdHash = DigestUtils.md5Hex(signature);
        String mdToBase = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mdHash.getBytes(StandardCharsets.UTF_8));

        return encodedRequestBody.concat("." + mdToBase);
    }
}
