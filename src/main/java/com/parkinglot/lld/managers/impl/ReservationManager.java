package com.parkinglot.lld.managers.impl;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.parkinglot.lld.managers.IReservationManager;
import com.parkinglot.lld.managers.ISlotManager;
import com.parkinglot.lld.models.Reservation;
import com.parkinglot.lld.models.Ticket;
import com.parkinglot.lld.models.Vehicle;

public class ReservationManager implements IReservationManager {

    private static final int RESERVATION_DURATION = 15 * 60 * 1000; // 15 minutes in milliseconds
    private final ISlotManager slotManager;
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();


    public ReservationManager(final ISlotManager slotManager) {
        this.slotManager = slotManager;
    }

    @Override
    public Reservation reserve(final Vehicle vehicle, final int startTime) {
        final Optional<Ticket> slot = slotManager.reserve(vehicle, startTime);
        if (slot.isEmpty()) {
            throw new RuntimeException("No available parking slot");
        }

        final Reservation reservation = Reservation.builder()
                .reservationId(UUID.randomUUID().toString())
                .vehicleLicenseNumber(vehicle.getLicenseNumber())
                .startTime(startTime - RESERVATION_DURATION)
                .endTime(startTime + RESERVATION_DURATION)
                .parkingSlotId(slot.get().getSlotId())
                .build();
        reservations.put(reservation.getReservationId(), reservation);
        return reservation;
    }

    @Override
    public void cancelReservation(final Reservation reservation) {
        reservations.remove(reservation.getReservationId());
        slotManager.release(reservation.getParkingSlotId());
    }

}
