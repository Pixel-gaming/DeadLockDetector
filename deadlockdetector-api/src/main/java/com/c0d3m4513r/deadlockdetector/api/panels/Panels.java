package com.c0d3m4513r.deadlockdetector.api.panels;

import com.c0d3m4513r.deadlockdetector.api.Panel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public enum Panels {
    Pterodactyl(new Pterodactyl()),
    CraftyController(new com.c0d3m4513r.deadlockdetector.api.panels.CraftyController());

    @NonNull
    final Panel panel;
}
