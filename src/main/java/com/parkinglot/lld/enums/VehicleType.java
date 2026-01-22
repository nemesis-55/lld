package com.parkinglot.lld.enums;

import java.util.Set;

public enum VehicleType {
    BIKE(Set.of(SlotType.SMALL, SlotType.MEDIUM, SlotType.LARGE, SlotType.EV)),
    CAR(Set.of(SlotType.MEDIUM, SlotType.LARGE, SlotType.EV)),
    TRUCK(Set.of(SlotType.LARGE)),
    EV(Set.of(SlotType.EV));

    private final Set<SlotType> compatibleSlots;

    VehicleType(Set<SlotType> compatibleSlots) {
        this.compatibleSlots = compatibleSlots;
    }

    public Set<SlotType> compatibleSlotTypes() {
        return compatibleSlots;
    }
}
