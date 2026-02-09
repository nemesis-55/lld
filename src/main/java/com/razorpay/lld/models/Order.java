package com.razorpay.lld.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class Order {
    @Getter
    @Setter
    double netAmount;
    @Getter
    private final List<Items> selectedItems = new ArrayList<>();
    @Setter
    @Getter
    private final boolean isPaid = false;

    public Order(List<Items> selectedItems) {
        this.selectedItems.addAll(selectedItems);
        this.netAmount = calculateNetAmount();
    }

    private double calculateNetAmount() {
        return selectedItems.stream().mapToDouble(Items::getPrice).sum();
    }
}
