package com.parkinglot.lld.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class ParkingLevel {
    @Getter
    private final String levelId;
    private final List<ParkingSlot> parkingSlots = new ArrayList<>();

    public ParkingLevel(final String levelId, final List<ParkingSlot> slots) {
        parkingSlots.addAll(slots);
        this.levelId = levelId;
    }

    public void addParkingSlot(ParkingSlot slot) {
        parkingSlots.add(slot);
    }

    public void removeParkingSlot(ParkingSlot slot) {
        parkingSlots.remove(slot);
    }

    public List<ParkingSlot> getParkingSlots() {
        return List.copyOf(parkingSlots);
    }
}
