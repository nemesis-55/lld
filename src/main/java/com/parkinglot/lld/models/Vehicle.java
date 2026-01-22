package com.parkinglot.lld.models;

import com.parkinglot.lld.enums.VehicleType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Vehicle {
    private final String licenseNumber;
    private final VehicleType vehicleType;
}
