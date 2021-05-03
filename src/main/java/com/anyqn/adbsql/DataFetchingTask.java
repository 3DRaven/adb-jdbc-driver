package com.anyqn.adbsql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

@Slf4j
public class DataFetchingTask extends TimerTask {
    public static final Semaphore attachBlocker = new Semaphore(1);
    private boolean attached = false;
    private final JadbConnection jadb;
    private final PreparedStatement insertDeviceStatement;
    private final PreparedStatement deleteDevicesStatement;
    private final String deviceIdentifier;
    private PreparedStatement deletePackagesStatement;
    private PreparedStatement insertPackageStatement;
    private PreparedStatement insertFileStatement;
    private PreparedStatement deleteFilesStatement;
    private PreparedStatement attachDbStatement;
    private PreparedStatement detachDbStatement;
    private final Pattern pattern = Pattern.compile("^(package:)(.*)=(.*)$", Pattern.MULTILINE);
    private final String dbName;

    private final String packageName;

    private final Map<String, String> params;

    private OffsetDateTime lastModifiedDbDate;

    private File lastDbCopy;

    public DataFetchingTask(@NonNull final String deviceIdentifier, @NonNull final String packageName,
            @NonNull final String dbName, @NonNull final Map<String, String> params,
            @NonNull final Connection metadataDbConnection, @NonNull final JadbConnection jadb) throws SQLException {
        this.params = params;
        this.packageName = packageName;
        this.dbName = dbName;
        this.jadb = jadb;
        this.deviceIdentifier = deviceIdentifier;
        try {
            insertDeviceStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("insert-device.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            deleteDevicesStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("delete-devices.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            deletePackagesStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("delete-packages.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            insertPackageStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("insert-package.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            deleteFilesStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("delete-files.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            insertFileStatement = metadataDbConnection.prepareStatement(
                    IOUtils.resourceToString("insert-file.sql", Charset.defaultCharset(), getClass().getClassLoader()));

            attachDbStatement = metadataDbConnection.prepareStatement(
                    IOUtils.resourceToString("attach-db.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            detachDbStatement = metadataDbConnection.prepareStatement(
                    IOUtils.resourceToString("detach-db.sql", Charset.defaultCharset(), getClass().getClassLoader()));
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to load insert-device.sql", e);
        }
    }

    private void attachNewDb(final Optional<File> newDbFile) throws InterruptedException, SQLException {
        if (newDbFile.isPresent()) {
            attachBlocker.acquire();
            if (attached) {
                detachDbStatement.executeUpdate();
            }
            if (null != lastDbCopy && lastDbCopy.exists()) {
                lastDbCopy.delete();
            }
            lastDbCopy = newDbFile.get();
            attachDbStatement.setString(1, lastDbCopy.getAbsolutePath());
            attachDbStatement.executeUpdate();
            attached = true;
            attachBlocker.release();
        }
    }

    private Optional<File> getUpdatedDb(final JadbDevice device, final Map<String, String> applicationPathes)
            throws IOException, JadbException, FileNotFoundException, InterruptedException, SQLException {
        final String absoluteDbFilePath = applicationPathes.get(packageName) + dbName;

        // 2021-05-01 22:07:31.961255952 +0200
        final String stateResponse = StringUtils.trim(IOUtils.toString(
                device.executeShell(String
                        .format("run-as %s stat -c %%y %s%s", packageName, applicationPathes.get(packageName), dbName)),
                Charset.defaultCharset()));

        final OffsetDateTime readedModifiedDbDate = OffsetDateTime.parse(stateResponse,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn x"));

        if (null == lastModifiedDbDate || !readedModifiedDbDate.isEqual(lastModifiedDbDate)) {
            final String dbBase64 = StringUtils.trim(IOUtils.toString(
                    device.executeShell(String.format("run-as %s base64 -w 0 %s", packageName, absoluteDbFilePath)),
                    Charset.defaultCharset()));
            final File tmpDbFile = File.createTempFile(dbName, "");
            try (final FileOutputStream out = new FileOutputStream(tmpDbFile)) {
                out.write(Base64.getDecoder().decode(dbBase64));
            }

            return Optional.of(tmpDbFile);
        }

        return Optional.empty();
    }

    private boolean isDeviceAllowed(final JadbDevice device, final Optional<String> deviceName) {
        return Objects.equals(deviceIdentifier, "*") || Objects.equals(deviceName.orElse(null), deviceIdentifier)
                || Objects.equals(device.getSerial(), deviceIdentifier);
    }

    @Override
    public void run() {
        try {
            final List<JadbDevice> devices = jadb.getDevices();
            deleteDevicesStatement.executeUpdate();
            deletePackagesStatement.executeUpdate();
            for (final JadbDevice device : devices) {
                final Optional<String> deviceName = updateDeviceInfo(device);
                if (isDeviceAllowed(device, deviceName)) {
                    final Map<String, String> applicationPathes = updatePackagesList(device, packageName);
                    attachNewDb(getUpdatedDb(device, applicationPathes));
                    if ("true".equals(params.get("showAllFilesInPackage"))) {
                        updateFilesList(device, applicationPathes);
                    }
                }
            }
        } catch (IOException | JadbException | SQLException e) {
            throw new IllegalStateException("Unable to get data from device:", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Runnable thread interrupted:", e);
        }
    }

    private Optional<String> updateDeviceInfo(final JadbDevice device) {
        try {
            final String deviceName = StringUtils
                    .trim(IOUtils.toString(device.executeShell("getprop ro.product.model"), Charset.defaultCharset()));
            log.debug("Found device with name [{}] and serial [{}]", deviceName, device.getSerial());
            insertDeviceStatement.setString(1, null);
            insertDeviceStatement.setString(2, deviceName);
            insertDeviceStatement.setString(3, device.getSerial());
            insertDeviceStatement.executeUpdate();
            return Optional.of(deviceName);
        } catch (final Exception e) {
            log.error("Fetching data from device with serial [{}] get error [{}]", device.getSerial(), e);
            return Optional.empty();
        }
    }

    private void updateFilesList(final JadbDevice device, final Map<String, String> applicationPathes) {
        for (final Entry<String, String> path : applicationPathes.entrySet()) {
            try {
                final String applicationDataPath = path.getValue();
                final String applicationPackageName = path.getKey();
                final List<String> response = IOUtils.readLines(
                        device.executeShell(String
                                .format("run-as %s ls -R -C -m -p %s", applicationPackageName, applicationDataPath)),
                        Charset.defaultCharset());

                String lastPath = null;

                for (final String line : response) {
                    if (!line.startsWith("run-as: package not debuggable")
                            && !line.endsWith("No such file or directory")) {
                        if (line.startsWith(applicationDataPath) && line.endsWith(":")) {
                            lastPath = line.substring(0, line.length() - 1);
                        } else {
                            final String[] files = line.split(",");
                            for (final String fileName : files) {
                                final String trimmedFileName = StringUtils.trim(fileName);
                                if (StringUtils.isNotBlank(trimmedFileName) && !trimmedFileName.endsWith("/")) {
                                    insertFileStatement.setString(1, null);
                                    insertFileStatement.setString(2, device.getSerial());
                                    insertFileStatement.setString(3, applicationPackageName);
                                    insertFileStatement.setString(4, lastPath);
                                    insertFileStatement.setString(5, trimmedFileName);
                                    insertFileStatement.setString(6, lastPath + "/" + trimmedFileName);
                                    insertFileStatement.executeUpdate();
                                }
                            }
                        }
                    }
                }
            } catch (final Exception e) {
                log.error("Fetching files from device with serial [{}] get error [{}]", device.getSerial(), e);
            }
        }
    }

    private Map<String, String> updatePackagesList(@NonNull final JadbDevice device, final String allowedPackageName) {
        final Map<String, String> paths = new HashMap<>();
        try {
            List<String> packagesList;
            if (Objects.equals(allowedPackageName, "*")) {
                packagesList = IOUtils.readLines(device.executeShell("pm list packages -3"), Charset.defaultCharset());
            } else {
                packagesList = List.of("package:" + allowedPackageName);
            }
            for (final String pkgStr : packagesList) {

                final String packageName = pkgStr.substring(8);
                final List<String> dumpPackageInfo = IOUtils
                        .readLines(device.executeShell("dumpsys package " + packageName), Charset.defaultCharset());
                log.trace("Found package info {}", dumpPackageInfo);
                // dataDir=/data/user/0/com.amazon.mShop.android.shopping
                final String dataDirProperty = dumpPackageInfo.stream()
                        .filter(l -> l.contains("dataDir"))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException(
                                String.format("Data dir not found {}", dumpPackageInfo)));
                final String[] splittedProperty = dataDirProperty.split("=");
                if (splittedProperty.length != 2) {
                    throw new IllegalArgumentException(String.format("Error in data dir parsing {}", dataDirProperty));
                }

                final String applicationDataPath = StringUtils.trim(splittedProperty[1]);
                paths.put(packageName, applicationDataPath);
                log.debug("Found in allowed device [{}] package [{}] with path [{}]",
                        device.getSerial(),
                        packageName,
                        applicationDataPath);

                insertPackageStatement.setString(1, null);
                insertPackageStatement.setString(2, device.getSerial());
                insertPackageStatement.setString(3, packageName);
                insertPackageStatement.setString(3, applicationDataPath);
                insertPackageStatement.executeUpdate();

            }

        } catch (final Exception e) {
            log.error("Fetching packages from device with serial [{}] get error [{}]", device.getSerial(), e);
        }
        return paths;
    }
}
