package com.anyqn.adbjdbc;

import java.util.Map;
import java.util.TimerTask;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;

@Slf4j
public abstract class AbstractFetchingTask extends TimerTask {
    @Getter
    private final JadbConnection jadb;
    @Getter
    private final String deviceIdentifier;
    @Getter
    private final @NonNull Map<String, String> params;
    @Getter
    private final @NonNull String packageName;

    public AbstractFetchingTask(@NonNull final String deviceIdentifier, @NonNull final String packageName,
            @NonNull final Map<String, String> params, @NonNull final JadbConnection jadb) {
        log.debug("Called");
        this.packageName = packageName;
        this.params = params;
        this.jadb = jadb;
        this.deviceIdentifier = deviceIdentifier;
    }

}
