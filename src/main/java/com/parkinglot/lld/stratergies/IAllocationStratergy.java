package com.parkinglot.lld.stratergies;

import java.util.Optional;
import java.util.Set;

import com.parkinglot.lld.models.ParkingSlot;

public interface IAllocationStratergy {

    Optional<String> findSlot(Set<ParkingSlot> freeSlots);
}
