package com.razorpay.lld.models;

import com.razorpay.lld.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor
@Getter
public class Category {
    private final CategoryType categoryType;
    private final Set<Items> items;

    public final void addItem(Items item) {
        this.items.add(item);
    }

    public final void removeItem(Items item) {
        this.items.remove(item);
    }
}
