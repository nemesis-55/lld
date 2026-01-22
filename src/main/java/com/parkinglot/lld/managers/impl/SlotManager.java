package com.parkinglot.lld.managers.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.parkinglot.lld.enums.SlotState;
import com.parkinglot.lld.enums.SlotType;
import com.parkinglot.lld.enums.VehicleType;
import com.parkinglot.lld.managers.IAllocationJournal;
import com.parkinglot.lld.managers.ISlotManager;
import com.parkinglot.lld.models.JournalEntry;
import com.parkinglot.lld.models.ParkingLevel;
import com.parkinglot.lld.models.ParkingLot;
import com.parkinglot.lld.models.ParkingSlot;
import com.parkinglot.lld.models.Ticket;
import com.parkinglot.lld.models.Vehicle;
import com.parkinglot.lld.stratergies.IAllocationStratergy;

public class SlotManager implements ISlotManager {
    private Map<String, ParkingSlot> parkingSlots = new ConcurrentHashMap<>();
    private Map<SlotType, Set<String>> freeSlotsByType = new ConcurrentHashMap<>();
    private IAllocationStratergy allocationStratergy;
    private IAllocationJournal journal;
    private TicketManager ticketManager;

    public SlotManager(final ParkingLot parkingLot, final IAllocationStratergy allocationStratergy,
            final IAllocationJournal journal, final TicketManager ticketManager) {
        populateParkingSlots(parkingLot);
        this.allocationStratergy = allocationStratergy;
        this.journal = journal;
        this.ticketManager = ticketManager;
    }

    @Override
    public Optional<Ticket> allocate(final Vehicle vehicle, final String ctx) {

        final String allocationkey = IAllocationJournal.JOURNAL_KEY_FORMAT.formatted(vehicle.getLicenseNumber(), ctx);

        Optional<JournalEntry> entryOpt = journal.get(allocationkey);
        if (entryOpt.isPresent() && entryOpt.get().isCommitted()) {
            // Allocation already happened earlier
            return ticketManager.getByAllocationKey(allocationkey);
        }

        final Set<ParkingSlot> availableSlotIds = getSlotByVehicleType(vehicle.getVehicleType());

        final Optional<String> slotId = allocationStratergy.findSlot(availableSlotIds);

        if (slotId.isEmpty()) {
            return Optional.empty();
        }

        journal.recordIntent(allocationkey, slotId.get());

        final ParkingSlot slot = parkingSlots.get(slotId.get());
        if (slot.occupy(vehicle.getLicenseNumber())) {
            freeSlotsByType.get(slot.getSlotType()).remove(slotId.get());
            journal.commit(allocationkey);
            final Ticket ticket = ticketManager.createTicket(vehicle, slot, allocationkey);
            return Optional.of(ticket);
        }
        journal.rollback(allocationkey);
        return Optional.empty();
    }

    @Override
    public Optional<Ticket> reserve(final Vehicle vehicle, final int startTime) {
        final String reservationKey = IAllocationJournal.JOURNAL_KEY_FORMAT.formatted(vehicle.getLicenseNumber(),
                startTime);

        Optional<JournalEntry> entryOpt = journal.get(reservationKey);
        if (entryOpt.isPresent() && entryOpt.get().isCommitted()) {
            return ticketManager.getByAllocationKey(reservationKey);
        }

        final Set<ParkingSlot> candidates = getSlotByVehicleType(vehicle.getVehicleType());

        final Optional<String> slotIdOpt = allocationStratergy.findSlot(candidates);
        if (slotIdOpt.isEmpty()) {
            return Optional.empty();
        }

        final String slotId = slotIdOpt.get();

        final ParkingSlot slot = parkingSlots.get(slotId);

        journal.recordIntent(reservationKey, slotId);

        if (slot.reserve()) {
            freeSlotsByType.get(slot.getSlotType()).remove(slotId);
            journal.commit(reservationKey);
            final Ticket ticket = ticketManager.createTicket(vehicle, slot, reservationKey);
            return Optional.of(ticket);
        }

        journal.rollback(reservationKey);
        return Optional.empty();

    }

    @Override
    public void release(String slotId) {
        final Optional<ParkingSlot> slotOpt = Optional.ofNullable(parkingSlots.get(slotId));
        if (slotOpt.isEmpty()) {
            return;
        }
        final ParkingSlot slot = slotOpt.get();
        slot.free();
        freeSlotsByType.computeIfAbsent(parkingSlots.get(slotId).getSlotType(), k -> ConcurrentHashMap.newKeySet()).add(slotId);
    }

    private Set<ParkingSlot> getSlotByVehicleType(final VehicleType vehicleType) {
        final Set<SlotType> compatibleSlotTypes = vehicleType.compatibleSlotTypes();
        return getAvailableSlots(compatibleSlotTypes);
    }

    private void populateParkingSlots(final ParkingLot parkingLot) {
        for (final ParkingLevel level : parkingLot.getLevels()) {
            for (final ParkingSlot slot : level.getParkingSlots()) {
                parkingSlots.put(slot.getSlotId(), slot);
                if (slot.getState().get().compareTo(SlotState.FREE) == 0) {
                    freeSlotsByType.computeIfAbsent(slot.getSlotType(), k -> ConcurrentHashMap.newKeySet()).add(slot.getSlotId());
                }
            }
        }
    }

    private Set<ParkingSlot> getAvailableSlots(final Set<SlotType> slotTypes) {
        return slotTypes.stream()
                .flatMap(slotType -> freeSlotsByType.getOrDefault(slotType, Set.of()).stream())
                .map(slotId -> parkingSlots.get(slotId))
                .filter(this::isEligible)
                .collect(Collectors.toSet());
    }

    private boolean isEligible(final ParkingSlot slot) {
        return slot.getState().get().compareTo(SlotState.FREE) == 0;
    }

}
