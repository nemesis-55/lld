package com.razorpay.lld.models;

import com.razorpay.lld.enums.OfferType;
import lombok.AllArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
public abstract class Offer {
    private final OfferType offerType;
    @Setter
    private final boolean isActive;

    public abstract void applyOffer(Order order);
}
