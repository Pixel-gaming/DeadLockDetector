package com.c0d3m4513r.deadlockdetector.api.panels;

import com.c0d3m4513r.deadlockdetector.api.ActionSender;
import com.c0d3m4513r.deadlockdetector.api.Actions;
import com.c0d3m4513r.deadlockdetector.api.PanelInfo;
import com.c0d3m4513r.logger.Logger;
import lombok.NonNull;

@SuppressWarnings("unused")
class Panel implements com.c0d3m4513r.deadlockdetector.api.Panel {
    Panel(){}
    @Override
    public @NonNull String getUUID(@NonNull ActionSender sender, @NonNull PanelInfo info, @NonNull Logger logger) {
        return info.getUuid();
    }

    @Override
    public void power(@NonNull ActionSender sender, @NonNull Actions action, @NonNull PanelInfo info, @NonNull Logger logger) {
    }
}
