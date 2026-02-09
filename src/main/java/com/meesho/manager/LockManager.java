package com.meesho.manager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public ReentrantLock getLock(final String key) {
        lockMap.putIfAbsent(key, new ReentrantLock());
        return lockMap.get(key);
    }
}
