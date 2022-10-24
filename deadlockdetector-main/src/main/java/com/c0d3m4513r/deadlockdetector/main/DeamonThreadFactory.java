package com.c0d3m4513r.deadlockdetector.main;

import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@NoArgsConstructor
class DeamonThreadFactory implements ThreadFactory {
    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

    public Thread newThread(@NonNull Runnable r) {
        Thread t = defaultThreadFactory.newThread(r);
        if (!t.isDaemon())
            t.setDaemon(true);
        return t;
    }
}