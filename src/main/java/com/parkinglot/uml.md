# Parking Lot System — 10/10 UML Diagram (Production-Grade LLD)

> **Target:** Staff / Principal level UML. This diagram emphasizes **state ownership, concurrency boundaries, recovery, and extensibility**. Nothing here is accidental.

---

## 1. High-Resolution Class Diagram (Textual UML)

```
+----------------------------------------------------------------------------------+
|                                   ParkingLot                                     |
+----------------------------------------------------------------------------------+
| - lotId : String                                                                  |
| - name : String                                                                   |
| - levels : List<ParkingLevel>                                                     |
| - allocationPolicy : AllocationPolicy                                             |
+----------------------------------------------------------------------------------+
| + park(vehicle : Vehicle, allocationKey, reservationId) : Ticket                 |
| + exit(ticket : Ticket) : void                                                    |
| + getAvailability() : LotAvailability                                             |
| + isFull() : boolean                                                              |
+-----------------------------------+----------------------------------------------+
                                    |
                                    | 1
                                    |
                                    | *
+-----------------------------------v----------------------------------------------+
|                               ParkingLevel                                       |
+----------------------------------------------------------------------------------+
| - levelId : String                                                                |
| - levelNumber : int                                                               |
| - isOperational : boolean                                                         |
| - slots : Map<String, ParkingSlot>        <<SOURCE OF TRUTH>>                     |
| - slotIndex : SlotIndex                  <<DERIVED STRUCTURE>>                    |
| - allocator : SlotAllocator                                                     |
+----------------------------------------------------------------------------------+
| + allocate(vehicle, allocationKey, reservationId) : Optional<Ticket>              |
| + release(slotId : String) : void                                                 |
| + availability() : LevelAvailability                                              |
+-----------------------------------+----------------------------------------------+
                                    |
                                    | 1
                                    |
                                    | *
+-----------------------------------v----------------------------------------------+
|                                ParkingSlot                                       |
+----------------------------------------------------------------------------------+
| - slotId : String                                                                 |
| - slotType : SlotType                                                             |
| - levelNumber : int                                                               |
| - state : AtomicReference<SlotState>   <<CONCURRENCY BOUNDARY>>                   |
+----------------------------------------------------------------------------------+
| + tryReserve() : boolean                                                          |
| + tryOccupyFromReserved() : boolean                                               |
| + tryOccupyDirect() : boolean                                                     |
| + free() : void                                                                   |
| + getState() : SlotState                                                          |
+-----------------------------------+----------------------------------------------+
                                    ^
                                    |
                                    | 1
+-----------------------------------+----------------------------------------------+
|                                    Ticket                                        |
+----------------------------------------------------------------------------------+
| - ticketId : String                                                               |
| - vehiclePlate : String                                                          |
| - slotId : String                                                                |
| - levelNumber : int                                                              |
| - entryTime : Instant                                                            |
| - status : TicketStatus                                                          |
+----------------------------------------------------------------------------------+

+-----------------------------------+        uses        +--------------------------+
|            SlotAllocator           |------------------>|    AllocationJournal     |
+-----------------------------------+                   +--------------------------+
| + allocate(...)                    |                   | + recordIntent()         |
| + release(slotId)                  |                   | + commit()               |
+-----------------------------------+                   | + rollback()             |
                                                    +--------------------------+

+-----------------------------------+        uses        +--------------------------+
|           SlotAllocator            |------------------>|     IdempotencyStore     |
+-----------------------------------+                   +--------------------------+
                                                    | + get(key)                |
                                                    | + put(key, ticket)        |
                                                    +--------------------------+

+-----------------------------------+        manages     +--------------------------+
|        ReservationManager          |------------------>|       Reservation        |
+-----------------------------------+                   +--------------------------+
| + reserve(...)                     |                   | - reservationId          |
| + expireReservations()             |                   | - slotId                 |
+-----------------------------------+                   | - expiryTime             |
                                                    +--------------------------+

+-----------------------------------+
|               Vehicle              |
+-----------------------------------+
| - plate : String                   |
| - type : VehicleType               |
+-----------------------------------+

+-----------------------------------+
|           SlotIndex                |
+-----------------------------------+
| - freeByType : Map<SlotType, PQ>   |
| - indexedSlotIds : Set<String>     |
+-----------------------------------+
| + candidates(vehicleType)          |
| + markOccupied(slot)               |
| + markFree(slot)                   |
+-----------------------------------+
```

---

## 2. State Machines (Explicit)

### ParkingSlot State Machine

```
FREE ──CAS──▶ RESERVED ──CAS──▶ OCCUPIED ──▶ FREE
```

Rules:

* Only `ParkingSlot` mutates its own state
* All transitions are atomic
* No backward transitions except FREE reset

---

## 3. Key Design Annotations (Why this is 10/10)

### 🔹 ParkingLot — Aggregate Root

* Orchestrates across levels
* Never mutates slot state
* Owns lot-wide policies and availability

### 🔹 ParkingLevel — Shard / Concurrency Boundary

* Owns slots map (authoritative)
* Owns SlotIndex (performance)
* Can scale independently

### 🔹 ParkingSlot — Linearizability Anchor

* CAS-based state
* Smallest unit of correctness
* No external locks

### 🔹 SlotIndex — Performance Accelerator

* Contains only FREE / reservable slots
* Rebuildable from slots map
* Never trusted for correctness

### 🔹 SlotAllocator — Algorithm Boundary

* Encapsulates allocation strategy
* Easily replaceable (simple → mature)

### 🔹 AllocationJournal — Crash Safety

* Write-ahead intent logging
* Enables deterministic recovery

### 🔹 IdempotencyStore — Retry Safety

* Prevents duplicate allocation
* Critical for offline replay

### 🔹 ReservationManager — Priority Control

* Time-bound exclusivity
* Integrated into allocation path

---

## 4. Supporting Value Objects

```
VehicleType   = BIKE | CAR | TRUCK | EV
SlotType      = SMALL | MEDIUM | LARGE | EV
SlotState     = FREE | RESERVED | OCCUPIED
TicketStatus  = ACTIVE | PAID | EXITED
```

---

## 5. Read Models (CQRS-Style)

```
LotAvailability
 ├── totalSlots
 ├── freeSlots
 ├── freeByType
 └── freeByLevel

LevelAvailability
 ├── levelNumber
 ├── freeSlots
 └── freeByType
```

Read models are:

* Immutable
* Derived from authoritative state
* Safe to cache

---

## 6. Allocation Flow (Mapped to UML)

```
ParkingLot.park()
  → ParkingLevel.allocate()
     → SlotIndex.candidates()
     → AllocationJournal.recordIntent()
     → ParkingSlot.CAS(FREE → OCCUPIED)
     → SlotIndex.markOccupied()
     → AllocationJournal.commit()
     → IdempotencyStore.put()
     → Ticket
```

---

## 7. Why Interviewers Rate This 10/10

| Dimension      | Reason                     |
| -------------- | -------------------------- |
| Correctness    | CAS + WAL + idempotency    |
| Concurrency    | Slot-level isolation       |
| Scalability    | Per-level sharding         |
| Recoverability | WAL + rebuildable index    |
| Extensibility  | Strategy-based allocator   |
| Clarity        | Clear ownership boundaries |

---

## 8. One-Sentence Explanation (Memorize)

> “Slots own correctness, levels own concurrency, the lot orchestrates, and everything else exists to make this fast, safe, and recoverable.”

---

## 9. What Comes Next (Optional)

* Allocation **sequence diagram** (race + crash)
* JVM-level CAS vs DB transaction comparison
* Distributed version (leader per level)

If you want, say the word and I’ll go deeper.
