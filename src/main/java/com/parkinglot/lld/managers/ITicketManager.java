package com.parkinglot.lld.managers;

import java.util.Optional;

import com.parkinglot.lld.models.ParkingSlot;
import com.parkinglot.lld.models.Ticket;
import com.parkinglot.lld.models.Vehicle;

public interface ITicketManager {
    Ticket createTicket(
        Vehicle vehicle,
        ParkingSlot slot,
        String allocationKey
    );

    Optional<Ticket> getByAllocationKey(String allocationKey);

    void closeTicket(String ticketId);
}
