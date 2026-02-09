package com.meesho.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class Restaurant {
    private final String restaurantName;
    private final Set<Integer> pincodes;
    private final String foodItemName;
    private final double foodItemPrice;
    private AtomicReference<Double> averageRating;
    private final List<Rating> ratings;

    public Restaurant(String restaurantName, Set<Integer> pincodes, String foodItemName, double foodItemPrice) {
        this.restaurantName = restaurantName;
        this.pincodes = pincodes;
        this.foodItemName = foodItemName;
        this.foodItemPrice = foodItemPrice;
        this.averageRating = new AtomicReference<>(0.0);
        this.ratings = new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    public void addPincode(int pincode) {
        this.pincodes.add(pincode);
    }

    public void removePincode(int pincode) {
        this.pincodes.remove(pincode);
    }

    public void addRating(Rating rating) {
        double newRating = (averageRating.get() * ratings.size() + rating.getRating())/( ratings.size() + 1);
        averageRating.set(newRating);
        this.ratings.add(rating);
    }

    public void removeRating(Rating rating) {
        double newRating = (averageRating.get() * ratings.size() - rating.getRating())/( ratings.size() - 1);
        averageRating.set(newRating);
        this.ratings.remove(rating);
    }
}
