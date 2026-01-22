package com.parkinglot.lld.managers;

import java.util.Optional;

import com.parkinglot.lld.models.Ticket;
import com.parkinglot.lld.models.Vehicle;

public interface ISlotManager {
    Optional<Ticket> allocate(final Vehicle vehicle, final String ctx);
    void release(final String slotId);
    Optional<Ticket> reserve(final Vehicle vehicle, final int startTime);
} 