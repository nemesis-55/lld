package com.razorpay.lld.mangers;

import com.razorpay.lld.enums.CategoryType;
import com.razorpay.lld.models.Items;
import com.razorpay.lld.models.Store;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryManager {
    private final Store store;
    private final ConcurrentHashMap<CategoryType, Set<Items>> inventoryMap = new ConcurrentHashMap<>();

    public InventoryManager(final Store store) {
        this.store = store;
        store.getCategories().forEach(category -> {
            inventoryMap.put(category.getCategoryType(), category.getItems());
        });
    }

    public Set<Items> getItemsByCategory(final CategoryType categoryType) {
        return inventoryMap.get(categoryType);
    }

    public void addItemToCategory(final CategoryType categoryType, final Items item) {
        store.getCategories().forEach(category -> {
            if (category.getCategoryType().equals(categoryType)) {
                category.getItems().add(item);
                inventoryMap.computeIfAbsent(category.getCategoryType(), newCategoryType -> ConcurrentHashMap.newKeySet()).add(item);
            }
        });
    }
}
