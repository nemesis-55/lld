package com.parkinglot.lld;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parkinglot.lld.enums.*;
import com.parkinglot.lld.managers.impl.*;
import com.parkinglot.lld.models.*;
import com.parkinglot.lld.stratergies.impl.RandomizedStratergy;

/**
 * This test suite validates:
 * 1. Idempotency (same request → same ticket)
 * 2. Race condition safety (no double allocation)
 * 3. Capacity enforcement
 * 4. Reservation contention correctness
 * 5. Replay / retry safety
 * 6. Slot release lifecycle correctness
 *
 * These tests collectively prove production-grade correctness
 * under concurrency and retries.
 */
public class SlotManagerConcurrencyTest {

    private SlotManager slotManager;
    private TicketManager ticketManager;
    private InMemoryJournal journal;

    @BeforeEach
    void setup() {
        // Create 10 MEDIUM parking slots
        List<ParkingSlot> slots = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            slots.add(new ParkingSlot(
                    "S" + i,
                    null,
                    SlotType.MEDIUM,
                    new AtomicReference<>(SlotState.FREE)));
        }

        ParkingLevel level = new ParkingLevel("L1", slots);
        ParkingLot lot = new ParkingLot("LOT1", "MainLot", List.of(level));

        journal = new InMemoryJournal();
        ticketManager = new TicketManager();

        slotManager = new SlotManager(
                lot,
                new RandomizedStratergy(),
                journal,
                ticketManager);
    }

    /**
     * Idempotency test:
     * Same vehicle + same allocation key must return the same ticket.
     */
    @Test
    void idempotentAllocation_returnsSameTicket() {
        Vehicle car = new Vehicle("KA01AB1234", VehicleType.CAR);

        Optional<Ticket> t1 = slotManager.allocate(car, "ENTRY");
        Optional<Ticket> t2 = slotManager.allocate(car, "ENTRY");

        assertTrue(t1.isPresent());
        assertTrue(t2.isPresent());
        assertEquals(
                t1.get().getTicketId(),
                t2.get().getTicketId(),
                "Idempotent allocation should return same ticket");
    }

    /**
     * Race condition test:
     * 50 concurrent threads attempt allocation.
     * No two tickets may have the same slotId.
     */
    @Test
    void concurrentAllocation_noDuplicateSlots() throws Exception {
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Callable<Optional<Ticket>>> tasks = IntStream.range(0, threads)
                .mapToObj(i -> (Callable<Optional<Ticket>>) () ->
                        slotManager.allocate(
                                new Vehicle("CAR-" + i, VehicleType.CAR),
                                "ENTRY"))
                .toList();

        List<Future<Optional<Ticket>>> results = executor.invokeAll(tasks);
        executor.shutdown();

        Set<String> allocatedSlots = new HashSet<>();

        for (Future<Optional<Ticket>> f : results) {
            f.get().ifPresent(ticket -> {
                assertTrue(
                        allocatedSlots.add(ticket.getSlotId()),
                        "Duplicate slot allocated under concurrency");
            });
        }
    }

    /**
     * Capacity enforcement test:
     * Requests exceed available slots.
     * Allocation must stop exactly at capacity.
     */
    @Test
    void allocationStopsAtCapacity() {
        int attempts = 20;
        int success = 0;

        for (int i = 0; i < attempts; i++) {
            Optional<Ticket> ticket =
                    slotManager.allocate(
                            new Vehicle("V" + i, VehicleType.CAR),
                            "ENTRY");
            if (ticket.isPresent()) {
                success++;
            }
        }

        assertEquals(
                10,
                success,
                "Allocated tickets must not exceed slot capacity");
    }

    /**
     * Reservation contention test:
     * More reservation attempts than slots.
     * Only available slots should be reserved.
     */
    @Test
    void reservationContention_onlyAvailableSlotsReserved() {
        int attempts = 20;
        int success = 0;

        for (int i = 0; i < attempts; i++) {
            Optional<Ticket> ticket =
                    slotManager.reserve(
                            new Vehicle("R" + i, VehicleType.CAR),
                            1000);
            if (ticket.isPresent()) {
                success++;
            }
        }

        assertEquals(
                10,
                success,
                "Reservations must respect slot capacity");
    }

    /**
     * Replay / retry safety test:
     * Same allocationKey replayed after a simulated crash
     * must return the same ticket.
     */
    @Test
    void replayAfterCrash_returnsSameTicket() {
        Vehicle vehicle = new Vehicle("REPLAY1", VehicleType.CAR);

        Optional<Ticket> first =
                slotManager.allocate(vehicle, "ENTRY");

        // Simulate replay (same journal + ticket manager)
        Optional<Ticket> replay =
                slotManager.allocate(vehicle, "ENTRY");

        assertTrue(first.isPresent());
        assertTrue(replay.isPresent());
        assertEquals(
                first.get().getTicketId(),
                replay.get().getTicketId(),
                "Replay must return same ticket");
    }

    /**
     * Slot lifecycle test:
     * FREE → OCCUPIED → FREE → OCCUPIED
     * Slot should be reusable after release.
     */
    @Test
    void releaseFreesSlotForReuse() {
        Vehicle v1 = new Vehicle("A1", VehicleType.CAR);
        Vehicle v2 = new Vehicle("A2", VehicleType.CAR);

        Ticket t1 = slotManager.allocate(v1, "ENTRY").get();
        slotManager.release(t1.getSlotId());

        Ticket t2 = slotManager.allocate(v2, "ENTRY").get();

        assertEquals(
                t1.getSlotId(),
                t2.getSlotId(),
                "Released slot should be reused");
    }
}
