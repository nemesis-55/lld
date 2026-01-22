package com.parkinglot.lld.managers.impl;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.parkinglot.lld.enums.TicketStatus;
import com.parkinglot.lld.managers.ITicketManager;
import com.parkinglot.lld.models.ParkingSlot;
import com.parkinglot.lld.models.Ticket;
import com.parkinglot.lld.models.Vehicle;

public class TicketManager implements ITicketManager {

    private final Map<String, Ticket> ticketsById = new ConcurrentHashMap<>();
    private final Map<String, String> allocationKeyIndex = new ConcurrentHashMap<>();

    @Override
    public Ticket createTicket(
            Vehicle vehicle,
            ParkingSlot slot,
            String allocationKey) {
        String existingTicketId = allocationKeyIndex.get(allocationKey);
        if (existingTicketId != null) {
            return ticketsById.get(existingTicketId);
        }

        Ticket ticket = new Ticket(
                UUID.randomUUID().toString(),
                slot.getSlotId(),
                vehicle.getLicenseNumber(),
                allocationKey,
                TicketStatus.UNPAID);

        ticketsById.put(ticket.getTicketId(), ticket);
        allocationKeyIndex.put(allocationKey, ticket.getTicketId());

        return ticket;
    }

    @Override
    public Optional<Ticket> getByAllocationKey(String allocationKey) {
        return Optional.ofNullable(allocationKeyIndex.get(allocationKey))
                .map(ticketsById::get);
    }

    @Override
    public void closeTicket(String ticketId) {
        Ticket ticket = ticketsById.get(ticketId);
        if (ticket == null)
            return;

        Ticket closed = new Ticket(
                ticket.getTicketId(),
                ticket.getSlotId(),
                ticket.getVehicleId(),
                ticket.getAllocationKey(),
                TicketStatus.PAID);

        ticketsById.put(ticketId, closed);
    }
}
