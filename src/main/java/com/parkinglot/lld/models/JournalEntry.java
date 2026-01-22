package com.parkinglot.lld.models;

import com.parkinglot.lld.enums.JournalState;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class JournalEntry {
    private final String slotId;
    private final JournalState state;

    public JournalEntry commit() {
        return new JournalEntry(slotId, JournalState.COMMITT);
    }

    public static JournalEntry intent(final String slotId) {
        return new JournalEntry(slotId, JournalState.INTENT);
    }

    public boolean isCommitted() {
        return this.state == JournalState.COMMITT;
    }
}
