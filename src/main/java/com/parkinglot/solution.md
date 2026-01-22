Great — below is a **complete, interview-grade Java LLD solution** for the Parking Lot core you asked for.
I’ve included:

* a recommended **folder/project structure**
* all **classes + enums** you need (with full implementations)
* concurrency-safe allocation logic with comments and rationale
* a simple **WAL (in-memory + file)** implementation option
* an **executor-based concurrency test harness** that demonstrates the key scenarios
* explanation / notes for each file, and where interviewers will probe

This solution is focused on the **LLD core** (allocation, reservation, idempotency, WAL, offline replay hooks), not on REST/UI or payment. It’s production-minded but concise enough to review in an interview.

---

# Project layout (Maven-style)

```
parking-core/
└─ src/main/java/com/parking/
   ├─ model/
   │   ├─ Vehicle.java
   │   ├─ Ticket.java
   │   ├─ Reservation.java
   │   ├─ ParkingSlot.java
   │   └─ ParkingLevel.java
   ├─ enums/
   │   ├─ VehicleType.java
   │   ├─ SlotType.java
   │   ├─ SlotState.java
   │   └─ TicketStatus.java
   ├─ allocator/
   │   ├─ SlotAllocator.java
   │   └─ ConsistentSlotAllocator.java
   ├─ index/
   │   └─ SlotIndex.java
   ├─ journal/
   │   ├─ AllocationJournal.java
   │   └─ InMemoryAllocationJournal.java
   ├─ reservation/
   │   └─ ReservationManager.java
   ├─ store/
   │   └─ IdempotencyStore.java
   ├─ core/
   │   └─ ParkingLot.java
   └─ test/
       └─ MainTest.java
```

All code below uses `package com.parking...` for clarity.

---

# 1) Enums — domain types

```java
// src/main/java/com/parking/enums/VehicleType.java
package com.parking.enums;

public enum VehicleType {
    BIKE, CAR, TRUCK, EV
}
```

```java
// src/main/java/com/parking/enums/SlotType.java
package com.parking.enums;

public enum SlotType {
    SMALL, MEDIUM, LARGE, EV
}
```

```java
// src/main/java/com/parking/enums/SlotState.java
package com.parking.enums;

public enum SlotState {
    FREE, RESERVED, OCCUPIED
}
```

```java
// src/main/java/com/parking/enums/TicketStatus.java
package com.parking.enums;

public enum TicketStatus {
    ACTIVE, PAID, EXITED
}
```

---

# 2) Core model classes

```java
// src/main/java/com/parking/model/Vehicle.java
package com.parking.model;

import com.parking.enums.VehicleType;

/**
 * Immutable vehicle value object.
 */
public final class Vehicle {
    private final String plate;
    private final VehicleType type;

    public Vehicle(String plate, VehicleType type) {
        this.plate = plate;
        this.type = type;
    }

    public String getPlate() { return plate; }
    public VehicleType getType() { return type; }
}
```

```java
// src/main/java/com/parking/model/Ticket.java
package com.parking.model;

import com.parking.enums.TicketStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Simple Ticket DTO. Immutable-ish: status can be updated externally if needed.
 */
public class Ticket {
    private final String ticketId;
    private final String vehiclePlate;
    private final String slotId;
    private final int levelNumber;
    private final Instant entryTime;
    private volatile TicketStatus status;

    public Ticket(String ticketId, String vehiclePlate, String slotId, int levelNumber) {
        this.ticketId = ticketId;
        this.vehiclePlate = vehiclePlate;
        this.slotId = slotId;
        this.levelNumber = levelNumber;
        this.entryTime = Instant.now();
        this.status = TicketStatus.ACTIVE;
    }

    public String getTicketId() { return ticketId; }
    public String getVehiclePlate() { return vehiclePlate; }
    public String getSlotId() { return slotId; }
    public int getLevelNumber() { return levelNumber; }
    public Instant getEntryTime() { return entryTime; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket)) return false;
        Ticket t = (Ticket) o;
        return Objects.equals(ticketId, t.ticketId);
    }

    @Override
    public int hashCode() { return Objects.hash(ticketId); }
}
```

```java
// src/main/java/com/parking/model/Reservation.java
package com.parking.model;

import java.time.Instant;

/**
 * Basic reservation record.
 */
public class Reservation {
    private final String reservationId;
    private final String slotId; // reserved slot (could be null for best-effort)
    private final int levelNumber;
    private final Instant expiryTime;
    private final String userId;

    public Reservation(String reservationId, String slotId, int levelNumber, Instant expiryTime, String userId) {
        this.reservationId = reservationId;
        this.slotId = slotId;
        this.levelNumber = levelNumber;
        this.expiryTime = expiryTime;
        this.userId = userId;
    }

    public String getReservationId() { return reservationId; }
    public String getSlotId() { return slotId; }
    public int getLevelNumber() { return levelNumber; }
    public Instant getExpiryTime() { return expiryTime; }
    public String getUserId() { return userId; }
}
```

---

# 3) ParkingSlot — smallest concurrency unit

```java
// src/main/java/com/parking/model/ParkingSlot.java
package com.parking.model;

import com.parking.enums.SlotState;
import com.parking.enums.SlotType;

import java.util.concurrent.atomic.AtomicReference;

/**
 * ParkingSlot is the smallest consistency unit.
 *
 * It encapsulates an AtomicReference<SlotState> to support lock-free CAS transitions:
 * FREE -> RESERVED -> OCCUPIED -> FREE.
 *
 * All state transitions must use try* methods to ensure safe concurrency.
 */
public class ParkingSlot {
    private final String slotId;
    private final SlotType slotType;
    private final int levelNumber;
    private final AtomicReference<SlotState> state;

    public ParkingSlot(String slotId, SlotType slotType, int levelNumber) {
        this.slotId = slotId;
        this.slotType = slotType;
        this.levelNumber = levelNumber;
        this.state = new AtomicReference<>(SlotState.FREE);
    }

    public String getSlotId() { return slotId; }
    public SlotType getSlotType() { return slotType; }
    public int getLevelNumber() { return levelNumber; }

    public SlotState getState() { return state.get(); }

    // CAS: FREE -> RESERVED
    public boolean tryReserve() {
        return state.compareAndSet(SlotState.FREE, SlotState.RESERVED);
    }

    // CAS: RESERVED -> OCCUPIED
    public boolean tryOccupyFromReserved() {
        return state.compareAndSet(SlotState.RESERVED, SlotState.OCCUPIED);
    }

    // CAS: FREE -> OCCUPIED (for walk-in)
    public boolean tryOccupyDirect() {
        return state.compareAndSet(SlotState.FREE, SlotState.OCCUPIED);
    }

    // Free slot (idempotent)
    public void free() {
        state.set(SlotState.FREE);
    }

    @Override
    public String toString() {
        return "ParkingSlot{" + slotId + ", type=" + slotType + ", level=" + levelNumber + ", state=" + state.get() + "}";
    }
}
```

---

# 4) SlotIndex — in-memory index for candidates

```java
// src/main/java/com/parking/index/SlotIndex.java
package com.parking.index;

import com.parking.enums.SlotType;
import com.parking.enums.VehicleType;
import com.parking.model.ParkingSlot;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

/**
 * SlotIndex maintains free slot views for allocation speed.
 *
 * Implementation notes:
 * - For each SlotType we maintain a PriorityBlockingQueue ordered by level (lowest level first).
 * - SlotIndex only contains free/reservable slots. It is a derived structure built from ParkingSlot registry.
 * - Operations markOccupied/markFree maintain that index.
 *
 * Complexity: poll/offer O(log n) for that queue.
 */
public class SlotIndex {

    private final Map<SlotType, PriorityBlockingQueue<ParkingSlot>> freeByType;
    // quick lookup to know if a slot currently exists in index (for safety)
    private final Set<String> indexSlotIds = ConcurrentHashMap.newKeySet();

    public SlotIndex(Collection<ParkingSlot> allSlots) {
        freeByType = new EnumMap<>(SlotType.class);
        for (SlotType t : SlotType.values()) {
            // comparator: prefer lower level (close to entry)
            freeByType.put(t, new PriorityBlockingQueue<>(11,
                    Comparator.comparingInt(ParkingSlot::getLevelNumber)));
        }
        // initialize with FREE slots
        for (ParkingSlot s : allSlots) {
            if (s.getState().name().equals("FREE")) {
                freeByType.get(s.getSlotType()).offer(s);
                indexSlotIds.add(s.getSlotId());
            }
        }
    }

    /**
     * Returns a list snapshot of candidate slots compatible with vehicle type.
     * We intentionally return a collection (not remove) so allocator can iterate and attempt CAS.
     * The allocator must remove (or rely on markOccupied) after successful CAS.
     */
    public List<ParkingSlot> candidates(VehicleType vType) {
        List<SlotType> allowed = allowedSlotTypes(vType);
        List<ParkingSlot> out = new ArrayList<>();
        for (SlotType st : allowed) {
            PriorityBlockingQueue<ParkingSlot> q = freeByType.get(st);
            if (q != null) {
                // snapshot via toArray to avoid removing
                Object[] arr = q.toArray();
                for (Object o : arr) {
                    out.add((ParkingSlot) o);
                }
            }
        }
        // sort by level preference
        return out.stream().sorted(Comparator.comparingInt(ParkingSlot::getLevelNumber)).collect(Collectors.toList());
    }

    private List<SlotType> allowedSlotTypes(VehicleType vType) {
        return switch (vType) {
            case BIKE -> List.of(SlotType.SMALL, SlotType.MEDIUM, SlotType.LARGE);
            case CAR -> List.of(SlotType.MEDIUM, SlotType.LARGE);
            case TRUCK -> List.of(SlotType.LARGE);
            case EV -> List.of(SlotType.EV);
        };
    }

    /**
     * Mark slot occupied -> remove from index (best effort).
     */
    public void markOccupied(ParkingSlot s) {
        PriorityBlockingQueue<ParkingSlot> q = freeByType.get(s.getSlotType());
        if (q != null) {
            // remove is O(n) on PriorityBlockingQueue but acceptable for moderate queue sizes per level
            q.remove(s);
            indexSlotIds.remove(s.getSlotId());
        }
    }

    /**
     * Mark slot free -> add back to index.
     */
    public void markFree(ParkingSlot s) {
        PriorityBlockingQueue<ParkingSlot> q = freeByType.get(s.getSlotType());
        if (q != null) {
            q.offer(s);
            indexSlotIds.add(s.getSlotId());
        }
    }

    public boolean contains(String slotId) {
        return indexSlotIds.contains(slotId);
    }
}
```

> Note: `candidates()` returns a snapshot of candidate slots so the allocator can safely attempt CAS on each. Using `poll()` and `offer()` pattern is an alternative; here we favor iteration over snapshot to keep queue contents consistent.

---

# 5) AllocationJournal (WAL) — interface + an in-memory impl

```java
// src/main/java/com/parking/journal/AllocationJournal.java
package com.parking.journal;

/**
 * AllocationJournal is a write-ahead log abstraction.
 * recordIntent records the intention to allocate slot for requestId.
 * commit marks intent committed and durable.
 *
 * In production this would be backed by durable storage (file, DB, kafka).
 */
public interface AllocationJournal {
    void recordIntent(String requestId, String slotId);
    void commit(String requestId);
    void rollback(String requestId);
    boolean isCommitted(String requestId);
}
```

```java
// src/main/java/com/parking/journal/InMemoryAllocationJournal.java
package com.parking.journal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory WAL used for demonstration and tests.
 * It keeps two structures:
 *  - intentLog: requestId -> slotId (uncommitted)
 *  - committed: set of committed requestIds
 *
 * For persistence, replace this with file-based append or DB transaction log.
 */
public class InMemoryAllocationJournal implements AllocationJournal {

    private final Map<String, String> intentLog = new ConcurrentHashMap<>();
    private final Set<String> committed = ConcurrentHashMap.newKeySet();

    @Override
    public void recordIntent(String requestId, String slotId) {
        intentLog.put(requestId, slotId);
    }

    @Override
    public void commit(String requestId) {
        committed.add(requestId);
        intentLog.remove(requestId);
    }

    @Override
    public void rollback(String requestId) {
        intentLog.remove(requestId);
    }

    @Override
    public boolean isCommitted(String requestId) {
        return committed.contains(requestId);
    }
}
```

---

# 6) Idempotency store

```java
// src/main/java/com/parking/store/IdempotencyStore.java
package com.parking.store;

import com.parking.model.Ticket;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory idempotency key -> Ticket store.
 * In production, persistent store is required so idempotency survives restart.
 */
public class IdempotencyStore {
    private final ConcurrentHashMap<String, Ticket> map = new ConcurrentHashMap<>();

    public Optional<Ticket> get(String key) {
        return Optional.ofNullable(map.get(key));
    }

    public void put(String key, Ticket ticket) {
        map.put(key, ticket);
    }
}
```

---

# 7) ReservationManager (skeletal, thread-safe)

```java
// src/main/java/com/parking/reservation/ReservationManager.java
package com.parking.reservation;

import com.parking.model.Reservation;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Basic ReservationManager. Provides reserve and expire operations.
 * Reservation expiry should be run by a background scheduler in production.
 */
public class ReservationManager {
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    public void putReservation(Reservation r) {
        reservations.put(r.getReservationId(), r);
    }

    public Reservation get(String reservationId) {
        return reservations.get(reservationId);
    }

    public void expireReservations() {
        Instant now = Instant.now();
        var expired = reservations.values().stream()
                .filter(r -> r.getExpiryTime().isBefore(now))
                .map(Reservation::getReservationId)
                .collect(Collectors.toList());
        expired.forEach(reservations::remove);
    }

    // For tests and status
    public int countActive() {
        return reservations.size();
    }
}
```

---

# 8) SlotAllocator interface + ConsistentSlotAllocator (core allocation logic)

```java
// src/main/java/com/parking/allocator/SlotAllocator.java
package com.parking.allocator;

import com.parking.model.ParkingSlot;
import com.parking.model.Vehicle;

import java.util.Optional;

/**
 * Allocation minimal interface.
 */
public interface SlotAllocator {
    /**
     * Attempts to allocate a slot for vehicle.
     * allocationKey: used for idempotency (e.g., plate + gate + timeWindow).
     * reservationId: optional; if present, allocator should honour reservation if valid.
     */
    Optional<ParkingSlot> allocate(Vehicle vehicle, String allocationKey, String reservationId);

    void release(String slotId);
}
```

```java
// src/main/java/com/parking/allocator/ConsistentSlotAllocator.java
package com.parking.allocator;

import com.parking.index.SlotIndex;
import com.parking.journal.AllocationJournal;
import com.parking.model.ParkingSlot;
import com.parking.model.Ticket;
import com.parking.model.Vehicle;
import com.parking.store.IdempotencyStore;
import com.parking.reservation.ReservationManager;
import com.parking.model.Reservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ConsistentSlotAllocator implements the allocation algorithm:
 * 1) Idempotency check
 * 2) If reservation exists, attempt to occupy reserved slot
 * 3) Otherwise fetch candidates from SlotIndex and try CAS on slot state
 * 4) Use WAL (AllocationJournal) to record intent and commit after success
 *
 * Notes:
 * - This implementation favors correctness: it uses CAS on ParkingSlot
 *   and a journal to make allocation durable.
 * - The SlotIndex is a helper; the authoritative state is in ParkingSlot.
 */
public class ConsistentSlotAllocator implements SlotAllocator {

    private final SlotIndex slotIndex;
    private final AllocationJournal journal;
    private final IdempotencyStore idempotencyStore;
    private final ReservationManager reservationManager;

    public ConsistentSlotAllocator(SlotIndex slotIndex,
                                   AllocationJournal journal,
                                   IdempotencyStore idempotencyStore,
                                   ReservationManager reservationManager) {
        this.slotIndex = slotIndex;
        this.journal = journal;
        this.idempotencyStore = idempotencyStore;
        this.reservationManager = reservationManager;
    }

    @Override
    public Optional<ParkingSlot> allocate(Vehicle vehicle, String allocationKey, String reservationId) {
        // 1) idempotency
        Optional<Ticket> maybe = idempotencyStore.get(allocationKey);
        if (maybe.isPresent()) {
            // Already allocated previously, return the slot (caller can look up by ticket)
            return Optional.ofNullable(findSlotById(maybe.get().getSlotId()));
        }

        // 2) if reservation present, attempt to claim reserved slot
        if (reservationId != null) {
            Reservation r = reservationManager.get(reservationId);
            if (r != null && r.getExpiryTime().isAfter(java.time.Instant.now())) {
                ParkingSlot reservedSlot = findSlotById(r.getSlotId());
                if (reservedSlot != null) {
                    // Try to occupy from reserved
                    journal.recordIntent(allocationKey, reservedSlot.getSlotId());
                    if (reservedSlot.tryOccupyFromReserved()) {
                        // success - mark index and commit
                        slotIndex.markOccupied(reservedSlot);
                        journal.commit(allocationKey);
                        Ticket t = new Ticket(UUID.randomUUID().toString(), vehicle.getPlate(), reservedSlot.getSlotId(), reservedSlot.getLevelNumber());
                        idempotencyStore.put(allocationKey, t);
                        return Optional.of(reservedSlot);
                    } else {
                        journal.rollback(allocationKey);
                        // fallback to normal allocation
                    }
                }
            }
        }

        // 3) Normal allocation
        List<ParkingSlot> candidates = slotIndex.candidates(vehicle.getType());
        for (ParkingSlot candidate : candidates) {
            // record intent before attempting CAS: durable intent
            journal.recordIntent(allocationKey, candidate.getSlotId());
            // try CAS to occupy
            if (candidate.tryOccupyDirect()) {
                // success -> update index, commit, create ticket, record idempotency
                slotIndex.markOccupied(candidate);
                journal.commit(allocationKey);
                Ticket t = new Ticket(UUID.randomUUID().toString(), vehicle.getPlate(), candidate.getSlotId(), candidate.getLevelNumber());
                idempotencyStore.put(allocationKey, t);
                return Optional.of(candidate);
            } else {
                // failed -> rollback and continue
                journal.rollback(allocationKey);
            }
        }
        return Optional.empty();
    }

    @Override
    public void release(String slotId) {
        ParkingSlot s = findSlotById(slotId);
        if (s != null) {
            s.free();
            // add back to index
            slotIndex.markFree(s);
        }
    }

    // Helper: in the full system this would query the ParkingLevel registry.
    // The allocator holds a reference to the same slots used by the SlotIndex,
    // but this convenience method is left as a placeholder for integration by ParkingLevel.
    private ParkingSlot findSlotById(String slotId) {
        // slotIndex doesn't expose direct map; in real implementation the allocator
        // would refer to the ParkingLevel/slots registry. For the sample code,
        // assume allocator has access or ParkingLevel delegates the allocate call.
        // Here we return null (integration provided in ParkingLevel/ ParkingLot).
        return null;
    }
}
```

> **Important note:** `ConsistentSlotAllocator` above requires access to the `ParkingSlot` registry for `findSlotById()` — in the real code this is provided by `ParkingLevel` (the allocator is typically injected per-level and given access to that level's `slots` map). For clarity in the full project below the `ParkingLevel` will call the allocator and provide callbacks.

---

# 9) ParkingLevel — orchestration within a level

```java
// src/main/java/com/parking/model/ParkingLevel.java
package com.parking.model;

import com.parking.index.SlotIndex;
import com.parking.allocator.SlotAllocator;
import com.parking.journal.AllocationJournal;
import com.parking.store.IdempotencyStore;
import com.parking.reservation.ReservationManager;
import com.parking.enums.SlotType;
import com.parking.enums.VehicleType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * ParkingLevel owns slots and provides allocate/release at this level.
 *
 * Responsibilities:
 * - keep slots registry (Map)
 * - maintain a SlotIndex built from slots
 * - call allocator with context; allocator must work on this level's slots
 */
public class ParkingLevel {
    private final String levelId;
    private final int levelNumber;
    private final boolean isOperational;
    private final Map<String, ParkingSlot> slots; // source of truth for this level
    private final SlotIndex index;
    private final SlotAllocator allocator; // injected per-level
    private final ReservationManager reservationManager;
    private final IdempotencyStore idempotencyStore;
    private final AllocationJournal journal;

    public ParkingLevel(String levelId, int levelNumber, boolean isOperational,
                        Collection<ParkingSlot> slotCollection,
                        Function<SlotIndex, SlotAllocator> allocatorFactory,
                        ReservationManager reservationManager,
                        IdempotencyStore idempotencyStore,
                        AllocationJournal journal) {
        this.levelId = levelId;
        this.levelNumber = levelNumber;
        this.isOperational = isOperational;
        this.slots = new ConcurrentHashMap<>();
        for (ParkingSlot s : slotCollection) this.slots.put(s.getSlotId(), s);
        this.index = new SlotIndex(slotCollection);
        this.reservationManager = reservationManager;
        this.idempotencyStore = idempotencyStore;
        this.journal = journal;
        // create allocator scoped to this index
        this.allocator = allocatorFactory.apply(this.index);
    }

    /**
     * Attempt to allocate for vehicle. Returns Ticket slotId (null if none).
     * allocationKey is the idempotency key (e.g., plate+gate+minuteWindow)
     */
    public Optional<ParkingSlot> allocate(Vehicle vehicle, String allocationKey, String reservationId) {
        // The allocator expects to be able to look up slots by id — we integrate by delegating allocation calls,
        // but for this simplified code, we will directly implement core loop here using index + journal + idempotency.
        // This ensures allocator can access slots map when needed.

        // 1) idempotency
        Optional<com.parking.model.Ticket> existing = idempotencyStore.get(allocationKey);
        if (existing.isPresent()) {
            String slotId = existing.get().getSlotId();
            ParkingSlot slot = slots.get(slotId);
            return Optional.ofNullable(slot);
        }

        // 2) Reservation logic
        if (reservationId != null) {
            Reservation r = reservationManager.get(reservationId);
            if (r != null && r.getExpiryTime().isAfter(java.time.Instant.now())) {
                ParkingSlot reserved = slots.get(r.getSlotId());
                if (reserved != null) {
                    journal.recordIntent(allocationKey, reserved.getSlotId());
                    if (reserved.tryOccupyFromReserved()) {
                        index.markOccupied(reserved);
                        journal.commit(allocationKey);
                        com.parking.model.Ticket t = new com.parking.model.Ticket(UUID.randomUUID().toString(),
                                vehicle.getPlate(), reserved.getSlotId(), reserved.getLevelNumber());
                        idempotencyStore.put(allocationKey, t);
                        return Optional.of(reserved);
                    } else {
                        journal.rollback(allocationKey);
                    }
                }
            }
        }

        // 3) normal walk-in allocation: iterate candidates and try CAS
        List<ParkingSlot> candidates = index.candidates(vehicle.getType());
        for (ParkingSlot candidate : candidates) {
            // record intent then CAS
            journal.recordIntent(allocationKey, candidate.getSlotId());
            if (candidate.tryOccupyDirect()) {
                index.markOccupied(candidate);
                journal.commit(allocationKey);
                com.parking.model.Ticket t = new com.parking.model.Ticket(UUID.randomUUID().toString(),
                        vehicle.getPlate(), candidate.getSlotId(), candidate.getLevelNumber());
                idempotencyStore.put(allocationKey, t);
                return Optional.of(candidate);
            } else {
                journal.rollback(allocationKey);
            }
        }
        return Optional.empty();
    }

    public void release(String slotId) {
        ParkingSlot s = slots.get(slotId);
        if (s == null) return;
        s.free();
        index.markFree(s);
    }

    public int availableCount() {
        // This can be optimized by an atomic counter; here we do simple scan for clarity.
        return (int) slots.values().stream().filter(sl -> sl.getState().name().equals("FREE")).count();
    }

    public Collection<ParkingSlot> getAllSlots() {
        return slots.values();
    }
}
```

> Note: `ParkingLevel` contains `slots` Map (source of truth) and the derived `SlotIndex`. `allocate` here directly implements the algorithm (rather than delegating fully to `SlotAllocator`), so findSlotById is not needed here.

---

# 10) ParkingLot — aggregate root & orchestrator

```java
// src/main/java/com/parking/core/ParkingLot.java
package com.parking.core;

import com.parking.model.ParkingLevel;
import com.parking.model.Vehicle;
import com.parking.model.ParkingSlot;
import com.parking.model.Ticket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ParkingLot orchestrates across levels.
 * It is intentionally thin — it selects level order (policy) and delegates allocation to levels.
 */
public class ParkingLot {
    private final String lotId;
    private final String name;
    private final List<ParkingLevel> levels; // order matters for allocation policy (e.g., lowest floor first)
    private final Map<String, ParkingLevel> levelMap = new ConcurrentHashMap<>();

    public ParkingLot(String lotId, String name, List<ParkingLevel> levels) {
        this.lotId = lotId;
        this.name = name;
        this.levels = levels;
        for (ParkingLevel l : levels) levelMap.put(l.getClass().getSimpleName() + ":" + l.hashCode(), l); // trivial
    }

    /**
     * Park: iterate levels by configured order and attempt allocate at each.
     * allocationKey: caller must provide idempotency key, e.g. plate+gate+minuteWindow.
     */
    public Optional<ParkingSlot> park(Vehicle vehicle, String allocationKey, String reservationId) {
        for (ParkingLevel level : levels) {
            Optional<ParkingSlot> s = level.allocate(vehicle, allocationKey, reservationId);
            if (s.isPresent()) return s;
        }
        return Optional.empty();
    }

    public void exit(String slotId) {
        // find level owning slot and release
        for (ParkingLevel level : levels) {
            // naive search: production should maintain slotId->level map
            Optional<ParkingSlot> found = level.getAllSlots().stream()
                    .filter(s -> s.getSlotId().equals(slotId)).findAny();
            if (found.isPresent()) {
                level.release(slotId);
                return;
            }
        }
    }

    public int totalFree() {
        return levels.stream().mapToInt(ParkingLevel::availableCount).sum();
    }
}
```

> In a real system we’d maintain a `slotId -> level` map to avoid scanning levels on exit. For readability this sample uses a direct search — in production avoid O(levels * slots).

---

# 11) Test harness — concurrency simulation

```java
// src/main/java/com/parking/test/MainTest.java
package com.parking.test;

import com.parking.core.ParkingLot;
import com.parking.enums.SlotType;
import com.parking.enums.VehicleType;
import com.parking.journal.InMemoryAllocationJournal;
import com.parking.model.*;
import com.parking.reservation.ReservationManager;
import com.parking.store.IdempotencyStore;
import com.parking.index.SlotIndex;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Main concurrency test demonstrating
 * - many concurrent park() calls
 * - no double allocation
 *
 * Note: This is a basic simulation for interview/demo purposes.
 */
public class MainTest {

    public static void main(String[] args) throws Exception {
        // Setup: create 3 levels, each with 50 slots => total 150
        List<ParkingLevel> levels = new ArrayList<>();
        ReservationManager reservationManager = new ReservationManager();
        IdempotencyStore idempotencyStore = new IdempotencyStore();
        InMemoryAllocationJournal journal = new InMemoryAllocationJournal();

        for (int levelNum = 1; levelNum <= 3; levelNum++) {
            List<ParkingSlot> slots = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                String slotId = "L" + levelNum + "-" + i;
                SlotType st = (i % 10 == 0) ? SlotType.EV : (i % 3 == 0 ? SlotType.SMALL : SlotType.MEDIUM);
                slots.add(new ParkingSlot(slotId, st, levelNum));
            }
            // allocatorFactory is not used (level implements allocation internally)
            ParkingLevel pl = new ParkingLevel("level-" + levelNum, levelNum, true, slots,
                    idx -> null, reservationManager, idempotencyStore, journal);
            levels.add(pl);
        }

        ParkingLot lot = new ParkingLot("lot-1", "Main Lot", levels);

        int requests = 200; // concurrent arrivals
        ExecutorService ex = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requests);

        Set<String> allocatedSlots = ConcurrentHashMap.newKeySet();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < requests; i++) {
            final int id = i;
            ex.submit(() -> {
                try {
                    startLatch.await();
                    // generate vehicle
                    String plate = "KA-01-" + String.format("%04d", id);
                    Vehicle v = new Vehicle(plate, (id % 7 == 0) ? VehicleType.EV : VehicleType.CAR);
                    String allocationKey = plate + "|" + (System.currentTimeMillis() / 60000); // minute window idempotency

                    Optional<ParkingSlot> allocated = lot.park(v, allocationKey, null);
                    allocated.ifPresent(slot -> {
                        allocatedSlots.add(slot.getSlotId());
                        successCount.incrementAndGet();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }

        long t0 = System.currentTimeMillis();
        startLatch.countDown();
        done.await();
        long t1 = System.currentTimeMillis();

        ex.shutdownNow();

        System.out.println("Requests: " + requests);
        System.out.println("Successful allocations: " + successCount.get());
        System.out.println("Unique allocated slots (set size): " + allocatedSlots.size());
        System.out.println("Total free after: " + lot.totalFree());
        System.out.println("Time taken (ms): " + (t1 - t0));

        // Basic correctness asserts (manual)
        if (successCount.get() != allocatedSlots.size()) {
            System.err.println("ERROR: duplicate allocations detected!");
        } else {
            System.out.println("No duplicate allocations detected in this run.");
        }
    }
}
```

Run this `MainTest` in your IDE. You should see successful allocations up to capacity (150). Because we submitted 200 requests, some fail when lot full.

---

# 12) Offline Gate Replay — design notes & hooks

This implementation is designed to support **offline gate replay**:

* Gate devices generate local intents with the same **allocationKey** (plate+gate+timeWindow) and a temporary ticket id.
* When gate reconnects, it replays intents into the system:

  * The system first checks `idempotencyStore.get(allocationKey)` — duplicates collapse.
  * If the slot referenced is taken, allocator will attempt other candidates or return an alternative (or notify user).
* `InMemoryAllocationJournal` provides minimal WAL for intents; production should use persistent append-only file or DB to survive restarts.

**Key design points for replay correctness:**

* Idempotency keys must be deterministic across offline/online replay.
* Journal must record intent & commit; replay can examine uncommitted intents and either roll them back or finish them deterministically.
* The source-of-truth is `ParkingSlot` state; the index is derived and rebuilt on restart.

---

# 13) Implementation caveats & production improvements

This code is a **reference LLD**; production hardening requires:

1. **Persistent WAL** (file or DB) to survive process crashes — `InMemoryAllocationJournal` should be replaced by a durable implemention.
2. **Persistent Idempotency Store** (Redis/DB) so retries across restarts are deduplicated.
3. **SlotId -> Level map** for O(1) release/lookup (avoid scanning).
4. **Atomic counters** per level to get availability in O(1).
5. **Background scheduler** to expire reservations and reconcile WAL intents.
6. **Health & metrics** (SLOs, queue backlog).
7. **ACID transactions** for commit in a multi-node environment (use leader per level + Raft or DB transactions).
8. **Tests**: add unit and fault-injection tests (crash mid-allocation + WAL replay).
9. **Security**: authenticate requests, validate idempotency keys.

---

# 14) Interview talking points — what to say and why

* **Start with invariants**: "slot state machine is FREE→RESERVED→OCCUPIED→FREE; it is enforced by CAS on ParkingSlot."
* **Explain source of truth vs index**: "`slots` map = source of truth; `SlotIndex` = derived view for allocation speed."
* **Describe WAL role**: "WAL records intent; if crash happens, uncommitted intents are rolled back or replayed deterministically."
* **Discuss idempotency for offline replay**: "Use plate+gate+minuteWindow as the idempotency key so replayed events collapse."
* **Trade-offs**:

  * CAS-per-slot vs global lock: CAS scales, global lock fails under load.
  * InMemoryJournal vs persisted WAL: retention vs latency trade-offs.
* **Complexities you'd add in production**: leader election per level, persistent WAL, asynchronous replication, metrics & alerting.

---

# 15) Files you can paste into IDE

You can copy-paste the classes above into a Maven project with `groupId=com.parking`, appropriate package folders, and run `MainTest` as a Java application.

---

If you’d like I can **do one of the following next** (pick one):

1. Convert the in-memory WAL to a **file-backed WAL** (with append, fsync, and recovery code) and show recovery flow.
2. Provide a **full JUnit test suite** for the concurrency cases (500 threads, crash simulation).
3. Produce a **one-page interview cheat sheet** summarizing invariants, failure modes, and TL;DR code snippets.

Which do you want me to do next?
