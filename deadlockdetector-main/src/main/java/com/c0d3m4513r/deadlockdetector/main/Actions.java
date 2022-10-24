package com.c0d3m4513r.deadlockdetector.main;

import lombok.AllArgsConstructor;
@AllArgsConstructor
public enum Actions {
    Start("start"),
    Stop("stop"),
    Restart("restart"),
    Kill("kill");
    public final String action;
}
