package com.parkinglot.lld.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ParkingLot {
    private final String id;
    private final String name;
    private final List<ParkingLevel> levels;

    public void addParkingLevel(ParkingLevel level) {
        levels.add(level);
    }

    public void removeParkingLevel(ParkingLevel level) {
        levels.remove(level);
    }
}
