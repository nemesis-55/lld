package com.parkinglot.lld.managers;

import java.util.Optional;

import com.parkinglot.lld.models.JournalEntry;

public interface IAllocationJournal {
    String JOURNAL_KEY_FORMAT = "%s_%s";
    void recordIntent(String slotId, String vehicleId);
    void commit(String slotId);
    void rollback(String slotId);
    Optional<JournalEntry> get(String Id);
}
