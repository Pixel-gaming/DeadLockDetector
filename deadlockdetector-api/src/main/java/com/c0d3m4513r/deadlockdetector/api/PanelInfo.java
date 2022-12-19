package com.c0d3m4513r.deadlockdetector.api;

import com.c0d3m4513r.deadlockdetector.api.panels.Panels;
import lombok.Value;

@Value
public class PanelInfo {
    //Nullable
    Panels panel;

    String panelUrl;
    Boolean ignore_ssl_cert_errors;
    String key;
    String uuid;
}
