package com.c0d3m4513r.deadlockdetector.api;

import com.c0d3m4513r.logger.Logger;
import lombok.NonNull;

public interface Panel {

    @NonNull
    String getUUID(@NonNull ActionSender sender, @NonNull PanelInfo info, @NonNull Logger logger);

    void power(@NonNull ActionSender sender, @NonNull Actions action, @NonNull PanelInfo info, @NonNull Logger logger);
}
