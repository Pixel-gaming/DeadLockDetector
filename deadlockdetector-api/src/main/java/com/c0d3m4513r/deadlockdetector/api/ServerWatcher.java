package com.c0d3m4513r.deadlockdetector.api;


public interface ServerWatcher {
    String heartbeat="HEARTBEAT";
    String config="CONFIG";
    String stopActions="STOP";
    String startActions="START";


    String shutdown="SHUTDOWN";
    String crash="CRASH";
}
