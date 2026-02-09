package com.meesho;

import com.meesho.enums.GenderType;
import com.meesho.enums.SortBy;
import com.meesho.models.Rating;
import com.meesho.models.Restaurant;
import com.meesho.models.User;
import com.meesho.service.RatingService;
import com.meesho.service.RestaurantService;
import com.meesho.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class BaseTest {
    private final UserService userService = new UserService();
    private final RestaurantService restaurantService = new RestaurantService();
    private final RatingService ratingService = new RatingService(userService, restaurantService);
    @Test
    public void initializationTest() {
        userService.registerUser("Pralove", GenderType.MALE, 1234567890L, 560035);
        userService.registerUser("Nitesh", GenderType.MALE, 1234567891L, 560088);
        userService.registerUser("Vatsal", GenderType.MALE, 1234567892L, 560088);

        restaurantService.registerRestaurant("Food Court-1", Set.of(560088,560035), "NI Thali", 100);
        restaurantService.registerRestaurant("Food Court-2", Set.of(560088), "Burger", 120);
        restaurantService.registerRestaurant("Food Court-3", Set.of(560035), "SI Thali", 150);


        User user = userService.getUserByPhoneNumber(1234567891L);
        Set<Restaurant> restaurants = restaurantService.getRestaurantsByPincodeDesc(user.getPincode(), SortBy.PRICE);
        Assertions.assertEquals(restaurants.size(), 2);

        ratingService.addRating(new Rating("Food Court-2", 3, "Good Food", 1234567891L));
        ratingService.addRating(new Rating("Food Court-1", 3, "Nice Food", 1234567892L));

        user = userService.getUserByPhoneNumber(1234567890L);
        restaurants = restaurantService.getRestaurantsByPincodeDesc(user.getPincode(), SortBy.RATING);
        Assertions.assertEquals(restaurants.size(), 2);


        List<Rating> rating = ratingService.getRatingsByRestaurantName("Food Court-3");
        Assertions.assertEquals(rating.size(), 1);
    }
}
