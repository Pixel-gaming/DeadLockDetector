package com.c0d3m4513r.deadlockdetector.api.panels.craftycontroller;

import lombok.Value;

@Value
public class ServerInformation {
    long server_id;
    String created;
    String server_uuid;
    String server_name;
    String path;
    String backup_path;
    String executable;
    String log_path;
    String execution_command;
    boolean auto_start;
    Long auto_start_delay;
    boolean crash_detection;
    String stop_command;
    String executable_update_url;
    int server_port;
    long logs_delete_after;
    String type;
}
