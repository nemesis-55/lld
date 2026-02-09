package com.razorpay.lld.mangers;

import com.razorpay.lld.models.Order;

import java.util.concurrent.ConcurrentHashMap;

public class OrderManager {
        private final ItemsManager itemsManager;
        private final DiscountManager discountManager;
        private final ConcurrentHashMap<String, Order> orderMap = new ConcurrentHashMap<>();

        public OrderManager(ItemsManager itemsManager, DiscountManager discountManager) {
            this.itemsManager = itemsManager;
            this.discountManager = discountManager;
        }

        public void calculateCheckoutAmout(Order order) {
            discountManager.calculateDiscountForItems(order);
        }
}
