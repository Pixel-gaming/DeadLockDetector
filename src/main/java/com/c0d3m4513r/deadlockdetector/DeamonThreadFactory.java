package com.c0d3m4513r.deadlockdetector;

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@NoArgsConstructor
class DeamonThreadFactory implements ThreadFactory {
    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

    public Thread newThread(@NotNull Runnable r) {
        Thread t = defaultThreadFactory.newThread(r);
        if (!t.isDaemon())
            t.setDaemon(true);
        return t;
    }
}