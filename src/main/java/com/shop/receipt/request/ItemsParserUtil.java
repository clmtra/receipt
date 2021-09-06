package com.shop.receipt.request;

import com.shop.receipt.model.Order;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ItemsParserUtil {
    private final Order order;

    public List<List<String>> getItems() {
        List<List<String>> items = new ArrayList<>();
        String productDescription = order.getProduct();

        if (productDescription.contains(";")) {
            String[] positionsArray = productDescription.split(";");
            for (String s : positionsArray) {
                items.add(getItem(s));
            }
        } else if (productDescription.contains("≡")) {
            items.add(getItem(productDescription));
        }

        return items;
    }

    private ArrayList<String> getItem(String product) {
        ArrayList<String> item = new ArrayList();
        item.add(parseProductName(product));
        item.add(parseQuantity(product));
        item.add(parseSumOfItem(product));
        item.add("none"); // tax: always none

        return item;
    }

    private String parseProductName(String product) {
        double percent;

        StringBuilder stringBuilder = new StringBuilder(product).reverse();
        product = stringBuilder.substring(stringBuilder.indexOf("-") + 1, stringBuilder.length());
        product = new StringBuilder(product).reverse().toString().trim();

        if ((percent = getDiscountPercent()) > 0) {
            product = product + " -" + String.format("%.2f", percent).replaceAll(",", ".") + "%";
        }

        return product;
    }


    private String parseQuantity(String product) {
        String quantityWithSum = getSubstringWithSum(product);
        char[] digitFinderArray = quantityWithSum.toCharArray();
        StringBuilder quantity = new StringBuilder(String.valueOf(digitFinderArray[0]));

        for (int i = 1; i < quantityWithSum.length(); i++) {
            if (Character.isDigit(quantityWithSum.charAt(i))) {
                quantity.append(quantityWithSum.charAt(i));
            } else {
                i = quantityWithSum.length();
            }
        }

        return quantity.toString();
    }

    private String parseSumOfItem(String product) {
        String sumWithQuantity = getSubstringWithSum(product);
        int beginIndex = sumWithQuantity.indexOf("x") + 1;
        int endIndex = sumWithQuantity.indexOf("≡");
        String sumWithoutDiscount = sumWithQuantity.substring(beginIndex, endIndex).trim();

        double discountPercent = getDiscountPercent();
        if (discountPercent > 0) {
            double sum = Double.parseDouble(sumWithoutDiscount);
            double sumAfterDiscount = sum - sum / 100 * discountPercent;
            System.out.println(String.format("%.2f", sumAfterDiscount).replaceAll(",", "."));

            return String.format("%.2f", sumAfterDiscount).replaceAll(",", ".");
        } else {
            return sumWithoutDiscount;
        }

    }

    private String getSubstringWithSum(String product) {
        String[] sumFinderArray = new StringBuilder(product).reverse().toString().split("-");

        return new StringBuilder(sumFinderArray[0]).reverse().toString().trim();
    }

    private double getDiscountPercent() {
        double total, delivery, subTotal, res;
        total = order.getTotalAmount();
        delivery = order.getDeliveryPrice();
        subTotal = order.getSubTotal();
        res = ((total - delivery) / subTotal) * 100;

        return 100 - res;
    }


}

