package com.razorpay.lld.mangers;

import com.razorpay.lld.models.Offer;
import com.razorpay.lld.models.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DiscountManager {
    private final List<Offer> offers = new ArrayList<>();
    private final ConcurrentHashMap<String, List<Offer>> itemOfferManager = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Offer>> categoryOfferManager = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Offer>> orderOfferManager = new ConcurrentHashMap<>();

    public void addOfferForItem(String itemName, Offer offer) {
        itemOfferManager.computeIfAbsent(itemName, k -> new ArrayList<>()).add(offer);
    }

    public void addOfferForCategory(String category, Offer offer) {
        itemOfferManager.computeIfAbsent(category, k -> new ArrayList<>()).add(offer);
    }

    public void addOfferForOrder(String orderId, Offer offer) {
        itemOfferManager.computeIfAbsent(orderId, k -> new ArrayList<>()).add(offer);
    }

    public List<Offer> getOffersForItem(String itemName) {
        return itemOfferManager.getOrDefault(itemName, new ArrayList<>());
    }

    public void calculateDiscountForItems(Order order) {
        order.getSelectedItems().forEach(item -> {
            List<Offer> offers = getOffersForItem(item.getName());
            offers.forEach(offer -> {
                offer.applyOffer(order);
            });
        });

    }

    public void calculateDiscountForCategory(String categoryName) {
        List<Offer> offers = categoryOfferManager.getOrDefault(categoryName, new ArrayList<>());
        // Logic to calculate discount based on the offers
    }

    public void calculateDiscountForOrder(String orderId) {
        List<Offer> offers = orderOfferManager.getOrDefault(orderId, new ArrayList<>());
        // Logic to calculate discount based on the offers
    }


}
