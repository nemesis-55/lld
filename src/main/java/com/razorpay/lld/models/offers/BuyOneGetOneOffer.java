package com.razorpay.lld.models.offers;

import com.razorpay.lld.enums.OfferType;
import com.razorpay.lld.models.Order;
import com.razorpay.lld.models.Items;
import com.razorpay.lld.models.Offer;

import java.util.HashMap;
import java.util.Map;

public class BuyOneGetOneOffer extends Offer {

    public BuyOneGetOneOffer() {
        super(OfferType.BUY_ONE_GET_ONE_FREE, true);
    }

    @Override
    public void applyOffer(Order order) {
        Map<Items, Integer> itemCountMap = new HashMap<>();
        order.getSelectedItems().forEach(item -> {
            itemCountMap.computeIfAbsent(item, k -> 0);
            itemCountMap.put(item, itemCountMap.get(item) + 1);
        });

        for (Map.Entry<Items, Integer> entry : itemCountMap.entrySet()) {
            Items item = entry.getKey();
            int count = entry.getValue();
            int freeItems = count / 2; // For every 2 items, 1 is free
            order.setNetAmount(order.getNetAmount() - (freeItems * item.getPrice()));
        }
    }

}
