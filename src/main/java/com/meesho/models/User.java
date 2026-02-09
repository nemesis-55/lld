package com.meesho.models;

import com.meesho.enums.GenderType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class User {
    private final String name;
    private final GenderType gender;
    private final Long phoneNumber;
    private final int pincode;
}
