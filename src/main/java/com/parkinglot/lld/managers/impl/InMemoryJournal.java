package com.parkinglot.lld.managers.impl;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.parkinglot.lld.managers.IAllocationJournal;
import com.parkinglot.lld.models.JournalEntry;

public class InMemoryJournal implements IAllocationJournal {

    private final ConcurrentMap<String, JournalEntry> journal = new ConcurrentHashMap<>();

    @Override
    public void recordIntent(final String key, final String slotId) {
        journal.putIfAbsent(key, JournalEntry.intent(slotId));
    }

    @Override
    public void commit(final String key) {
        journal.computeIfPresent(
            key,
            (k, entry) -> entry.commit()
        );
    }

    @Override
    public void rollback(final String key) {
        journal.remove(key);
    }

    @Override
    public Optional<JournalEntry> get(final String key) {
        return Optional.ofNullable(journal.get(key));
    }

}
