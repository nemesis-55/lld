package com.parkinglot.lld.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class Reservation {
    private final String reservationId;
    private final String parkingSlotId;
    private final String vehicleLicenseNumber;
    private final long startTime;
    private final long endTime;
}
