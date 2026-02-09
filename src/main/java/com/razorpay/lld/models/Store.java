package com.razorpay.lld.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class Store {
    private final String name;
    private final String address;
    @Getter
    private final List<Category> categories = new ArrayList<>();

    public void addCategory(Category category) {
        this.categories.add(category);
    }

    public void removeCategory(Category category) {
        this.categories.remove(category);
    }
}
