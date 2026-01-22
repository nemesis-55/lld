package com.parkinglot.lld.stratergies.impl;

import java.util.Optional;
import java.util.Set;

import com.parkinglot.lld.models.ParkingSlot;
import com.parkinglot.lld.stratergies.IAllocationStratergy;

public class RandomizedStratergy implements IAllocationStratergy {

    @Override
    public Optional<String> findSlot(Set<ParkingSlot> freeSlots) {
        if (freeSlots == null || freeSlots.isEmpty()) {
            return Optional.empty();
        }
        int randomIndex = (int) (Math.random() * freeSlots.size());
        return freeSlots.stream().skip(randomIndex).findFirst().map(ParkingSlot::getSlotId);
    }

}
