package com.parkinglot.lld.managers;

import com.parkinglot.lld.models.Reservation;
import com.parkinglot.lld.models.Vehicle;

public interface IReservationManager {

    Reservation reserve(final Vehicle vehicle, final int startTime);

    void cancelReservation(final Reservation reservation);

}
