package com.parkinglot.lld.models;

import com.parkinglot.lld.enums.TicketStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Ticket {
    private final String ticketId;
    private final String slotId;
    private final String vehicleId;
    private final String allocationKey;
    private final TicketStatus status;
}
