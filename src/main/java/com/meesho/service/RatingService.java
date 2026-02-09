package com.meesho.service;

import com.meesho.enums.SortBy;
import com.meesho.models.Rating;
import com.meesho.models.Restaurant;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@AllArgsConstructor
public class RatingService {
    private final UserService userService;
    private final RestaurantService restaurantService;

    public Set<Restaurant> getRestaurantsByPincode(int pincode, SortBy sortBy) {
        return restaurantService.getRestaurantsByPincodeDesc(pincode, sortBy);
    }

    public void addRating(Rating rating) {
        restaurantService.rateRestaurant(rating);
    }

    public List<Rating> getRatingsByRestaurantName(String restaurantName) {
        Optional<Restaurant> restaurantOpt = restaurantService.getRestaurantByName(restaurantName);
        return restaurantOpt.isPresent() ? restaurantOpt.get().getRatings() : List.of();
    }

}
