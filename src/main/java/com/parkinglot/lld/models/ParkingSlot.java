package com.parkinglot.lld.models;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.util.Strings;

import com.parkinglot.lld.enums.SlotState;
import com.parkinglot.lld.enums.SlotType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ParkingSlot {
    private final String slotId;
    private String vehicleId;
    private final SlotType slotType;
    private final AtomicReference<SlotState> state;

    public void free() {
        state.set(SlotState.FREE);
        vehicleId = null;
    }

    public boolean occupy(String vehicleId) {
        boolean updated = state.compareAndSet(SlotState.FREE, SlotState.OCCUPIED) || 
                          state.compareAndSet(SlotState.RESERVED, SlotState.OCCUPIED);
        if (updated && Strings.isEmpty(this.vehicleId)) {
            this.vehicleId = vehicleId;
        }
        return updated;
    }

    public boolean reserve() {
        return state.compareAndSet(SlotState.FREE, SlotState.RESERVED);
    }

}
