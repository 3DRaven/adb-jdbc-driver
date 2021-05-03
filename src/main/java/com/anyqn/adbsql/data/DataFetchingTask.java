package com.anyqn.adbsql.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.anyqn.adbsql.AbstractFetchingTask;
import com.anyqn.adbsql.ConnectionHolder;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

@Slf4j
public class DataFetchingTask extends AbstractFetchingTask {
    private final String dbName;
    private OffsetDateTime lastModifiedDbDate;
    private File lastDbCopy;
    private final ConnectionHolder connectionHolder;

    public DataFetchingTask(@NonNull final String deviceIdentifier, @NonNull final String packageName,
            @NonNull final String dbName, @NonNull final Map<String, String> params,
            @NonNull final ConnectionHolder connectionHolder, @NonNull final JadbConnection jadb) throws SQLException {
        super(deviceIdentifier, packageName, params, jadb);
        log.debug("Called");
        this.connectionHolder = connectionHolder;
        this.dbName = dbName;
    }

    private Optional<String> getDeviceName(final JadbDevice device) {
        log.debug("Called");
        try {
            return Optional.ofNullable(StringUtils
                    .trim(IOUtils.toString(device.executeShell("getprop ro.product.model"), Charset.defaultCharset())));
        } catch (final Exception e) {
            log.error("Fetching data from device with serial [{}] get error [{}]", device.getSerial(), e);
            return Optional.empty();
        }
    }

    private Optional<File> getUpdatedDb(@NonNull final JadbDevice device, @NonNull final String packageName,
            @NonNull final String dbAbsolutePath)
            throws IOException, JadbException, FileNotFoundException, InterruptedException, SQLException {
        log.debug("Called for trying to get db from android device with path {}", dbAbsolutePath);

        // 2021-05-01 22:07:31.961255952 +0200
        final String stateResponse = StringUtils.trim(IOUtils.toString(
                device.executeShell(String.format("run-as %s stat -c %%y %s", packageName, dbAbsolutePath)),
                Charset.defaultCharset()));

        log.debug("Loaded db file last state info {}", stateResponse);

        final OffsetDateTime readedModifiedDbDate = OffsetDateTime.parse(stateResponse,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn x"));

        if (null == lastModifiedDbDate || !readedModifiedDbDate.isEqual(lastModifiedDbDate)) {
            final String dbBase64 = StringUtils.trim(IOUtils.toString(
                    device.executeShell(String.format("run-as %s base64 -w 0 %s", packageName, dbAbsolutePath)),
                    Charset.defaultCharset()));
            log.debug("Db loaded from android device");
            final File tmpDbFile = File.createTempFile(dbName, null);
            try (final FileOutputStream out = new FileOutputStream(tmpDbFile)) {
                out.write(Base64.getDecoder().decode(dbBase64));
            }
            log.debug("Db saved to host as {}", tmpDbFile.getAbsolutePath());
            return Optional.of(tmpDbFile);
        }
        return Optional.empty();
    }

    private void replaceDb(final Optional<File> newDbFile) throws InterruptedException, SQLException {
        log.debug("Called");
        if (newDbFile.isPresent()) {
            connectionHolder.acquire();
            connectionHolder.close();
            if (null != lastDbCopy && lastDbCopy.exists()) {
                log.debug("Old db file copy delete {}", lastDbCopy.getAbsolutePath());
                lastDbCopy.delete();
            }
            lastDbCopy = newDbFile.get();
            connectionHolder.setConnection(DriverManager.getConnection("jdbc:sqlite:" + lastDbCopy.getAbsolutePath()));
            connectionHolder.release();
        }
        log.debug("Old db replaced");
    }

    @Override
    public void run() {
        log.debug("Called");
        try {
            log.debug("Get list of devices");
            final List<JadbDevice> devices = getJadb().getDevices();
            for (final JadbDevice device : devices) {
                log.debug("Process device with serial {}", device.getSerial());
                if (getDeviceIdentifier().equals(device.getSerial())
                        || getDeviceIdentifier().equals(getDeviceName(device).orElse(null))) {
                    replaceDb(getUpdatedDb(device,
                            getPackageName(),
                            getParams().getOrDefault("applicationDataRootPath", "/data/data/") + getPackageName()
                                    + dbName));
                }
            }
        } catch (IOException | JadbException | SQLException e) {
            throw new IllegalStateException("Unable to get data from device:", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Runnable thread interrupted:", e);
        }
    }

}
