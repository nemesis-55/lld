package com.meesho.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Rating {
    private final String restaurantName;
    private final int rating;
    private final String review;
    private final Long phoneNumber;
}
