package com.razorpay.lld.mangers;

import com.razorpay.lld.models.Items;

import java.util.concurrent.ConcurrentHashMap;

public class ItemsManager {
    private final ConcurrentHashMap<String, Items> itemsMap = new ConcurrentHashMap<>();

    public Items getItemById(String itemId) {
        return itemsMap.get(itemId);
    }

    public void addItem(Items item) {
        itemsMap.put(item.getId(), item);
    }


    public void removeItem(String itemId) {
        itemsMap.remove(itemId);
    }

}
