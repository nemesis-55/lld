package com.meesho.service;

import com.meesho.enums.SortBy;
import com.meesho.manager.LockManager;
import com.meesho.models.Rating;
import com.meesho.models.Restaurant;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@NoArgsConstructor
public class RestaurantService {
    private final ConcurrentHashMap<String, Restaurant> restaurantNameMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Set<Restaurant>> pincodeRestaurantMap = new ConcurrentHashMap<>();
    private final LockManager lockManager = new LockManager();

    public RestaurantService(final Set<Restaurant> restaurants) {
        for (Restaurant restaurant : restaurants) {
            restaurantNameMap.put(restaurant.getRestaurantName(), restaurant);
            restaurant.getPincodes().forEach(pincode -> {
                pincodeRestaurantMap.computeIfAbsent(pincode, k -> ConcurrentHashMap.newKeySet()).add(restaurant);
            });
        }
    }

    public void addRestaurant(final Restaurant restaurant) {
        lockManager.getLock(restaurant.getRestaurantName()).lock();
        try {
            restaurantNameMap.put(restaurant.getRestaurantName(), restaurant);
            restaurant.getPincodes().forEach(pincode -> {
                pincodeRestaurantMap.computeIfAbsent(pincode, k -> ConcurrentHashMap.newKeySet()).add(restaurant);
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lockManager.getLock(restaurant.getRestaurantName()).unlock();
        }
    }

    public void registerRestaurant(String restaurantName, Set<Integer> pincodes, String foodItemName, double foodItemPrice) {
        addRestaurant(new Restaurant(restaurantName, pincodes, foodItemName, foodItemPrice));
    }

    public void removeRestaurant(final String restaurantName) {
        lockManager.getLock(restaurantName).lock();
        try {
            restaurantNameMap.get(restaurantName).getPincodes().forEach(pincode -> {
                pincodeRestaurantMap.get(pincode).remove(restaurantNameMap.get(restaurantName));
            });
            restaurantNameMap.remove(restaurantName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lockManager.getLock(restaurantName).unlock();
        }
    }

    public void rateRestaurant(Rating rating) {
        restaurantNameMap.get(rating.getRestaurantName()).addRating(rating);
    }

    public Set<Restaurant> getRestaurantsByPincodeDesc(int pincode, SortBy sortBy) {
        Set<Restaurant> restaurants = pincodeRestaurantMap.getOrDefault(pincode, Collections.emptySet());
        return restaurants.stream()
                .sorted(comparatorFor(sortBy))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Optional<Restaurant> getRestaurantByName(String restaurantName) {
        return Optional.of(restaurantNameMap.get(restaurantName));
    }

    private Comparator<Restaurant> comparatorFor(SortBy sortBy) {
        Comparator<Restaurant> cmp;
        switch (sortBy) {
            case RATING:
                cmp = Comparator.comparingDouble(r -> r.getAverageRating().get());
                break;
            case PRICE:
            default:
                cmp = Comparator.comparingDouble(r -> r.getFoodItemPrice());
                break;
        }
        return cmp.reversed();
    }
}
