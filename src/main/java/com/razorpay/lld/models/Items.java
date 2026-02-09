package com.razorpay.lld.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class Items{

    private final String id;
    private final String name;
    private final double price;
    @Setter
    private int stock;
}
