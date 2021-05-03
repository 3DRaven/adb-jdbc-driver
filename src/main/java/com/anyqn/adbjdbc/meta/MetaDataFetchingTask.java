package com.anyqn.adbjdbc.meta;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.anyqn.adbjdbc.AbstractFetchingTask;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

@Slf4j
public class MetaDataFetchingTask extends AbstractFetchingTask {
    private final PreparedStatement deleteDevicesStatement;
    private PreparedStatement deletePackagesStatement;
    private PreparedStatement insertFileStatement;
    private final PreparedStatement insertDeviceStatement;
    private final PreparedStatement insertPackageStatement;
    private PreparedStatement deleteFilesStatement;

    public MetaDataFetchingTask(@NonNull final String deviceIdentifier, @NonNull final String packageName,
            @NonNull final Map<String, String> params, @NonNull final Connection metadataDbConnection,
            @NonNull final JadbConnection jadb) throws SQLException {
        super(deviceIdentifier, packageName, params, jadb);
        log.debug("Called");
        try {
            log.debug("Start to statement preparation");
            insertDeviceStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("insert-device.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            insertPackageStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("insert-package.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            insertFileStatement = metadataDbConnection.prepareStatement(
                    IOUtils.resourceToString("insert-file.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            deleteDevicesStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("delete-devices.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            deletePackagesStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("delete-packages.sql", Charset.defaultCharset(), getClass().getClassLoader()));
            deleteFilesStatement = metadataDbConnection.prepareStatement(IOUtils
                    .resourceToString("delete-files.sql", Charset.defaultCharset(), getClass().getClassLoader()));
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to create prepared statements", e);
        }
    }

    protected boolean isDeviceAllowed(final JadbDevice device, final Optional<String> deviceName) {
        log.debug("Called with device serial {} and device name {}", device.getSerial(), deviceName);
        return Objects.equals(getDeviceIdentifier(), "*")
                || Objects.equals(deviceName.orElse(null), getDeviceIdentifier())
                || Objects.equals(device.getSerial(), getDeviceIdentifier());
    }

    private List<String> readFilesList(final JadbDevice device, final String applicationDataPath,
            final String applicationPackageName) throws IOException, JadbException {
        final List<String> response = IOUtils.readLines(
                device.executeShell(
                        String.format("run-as %s ls -R -C -m -p %s", applicationPackageName, applicationDataPath)),
                Charset.defaultCharset());
        return response;
    }

    @Override
    public void run() {
        log.debug("Called");
        try {
            log.debug("Get list of devices");
            final List<JadbDevice> devices = getJadb().getDevices();
            deletePackagesStatement.executeUpdate();
            deleteDevicesStatement.executeUpdate();
            deleteFilesStatement.executeUpdate();
            for (final JadbDevice device : devices) {
                log.debug("Process device with serial {}", device.getSerial());
                final Optional<String> deviceName = updateDeviceInfo(device);
                if (isDeviceAllowed(device, deviceName)) {
                    final Map<String, String> applicationPathes = updatePackagesList(device, getPackageName());
                    updateFilesList(device, applicationPathes);
                }
            }
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to get data from device:", e);
        }
    }

    /**
     *
     * @param device
     * @return device name
     */
    protected Optional<String> updateDeviceInfo(final JadbDevice device) {
        log.debug("Called");
        try {
            final String deviceName = StringUtils
                    .trim(IOUtils.toString(device.executeShell("getprop ro.product.model"), Charset.defaultCharset()));
            log.debug("Found device with name [{}] and serial [{}]", deviceName, device.getSerial());
            insertDeviceStatement.setString(1, deviceName);
            insertDeviceStatement.setString(2, device.getSerial());
            insertDeviceStatement.executeUpdate();
            return Optional.of(deviceName);
        } catch (final Exception e) {
            log.error("Fetching data from device with serial [{}] get error [{}]", device.getSerial(), e);
            return Optional.empty();
        }
    }

    private void updateFilesList(final JadbDevice device, final Map<String, String> applicationPathes) {
        log.debug("Called");
        for (final Entry<String, String> path : applicationPathes.entrySet()) {
            try {
                final String applicationDataPath = path.getValue();
                final String applicationPackageName = path.getKey();
                log.debug("Application data path is {} package name is {}",
                        applicationDataPath,
                        applicationPackageName);

                final List<String> response = readFilesList(device, applicationDataPath, applicationPackageName);

                String lastPath = null;

                for (final String line : response) {
                    log.debug("File list string {}", line);
                    if (!line.startsWith("run-as: package not debuggable")
                            && !line.endsWith("No such file or directory")) {
                        if (line.startsWith(applicationDataPath) && line.endsWith(":")) {
                            log.debug("Is path");
                            lastPath = line.substring(0, line.length() - 1);
                        } else {
                            log.debug("Is files list");
                            final String[] files = line.split(",");
                            for (final String fileName : files) {
                                final String trimmedFileName = StringUtils.trim(fileName);
                                if (StringUtils.isNotBlank(trimmedFileName) && !trimmedFileName.endsWith("/")) {
                                    log.debug("Process file name {}", trimmedFileName);
                                    insertFileStatement.setString(1, device.getSerial());
                                    insertFileStatement.setString(2, applicationPackageName);
                                    insertFileStatement.setString(3, lastPath);
                                    insertFileStatement.setString(4, trimmedFileName);
                                    insertFileStatement.setString(5, lastPath + "/" + trimmedFileName);
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

    protected Map<String, String> updatePackagesList(@NonNull final JadbDevice device,
            final String allowedPackageName) {
        log.debug("Called");
        final Map<String, String> paths = new HashMap<>();
        try {
            List<String> packagesList;
            log.debug("Allowed package name is {}", allowedPackageName);
            if (Objects.equals(allowedPackageName, "*")) {
                packagesList = IOUtils.readLines(device.executeShell("pm list packages -3"), Charset.defaultCharset());
            } else {
                packagesList = List.of("package:" + allowedPackageName);
            }
            for (final String pkgStr : packagesList) {
                log.debug("Process package {}", pkgStr);
                final String packageName = pkgStr.substring(8);

                final List<String> response = readFilesList(device, "/data/data/" + packageName, packageName);
                if (!response.isEmpty() && !response.get(0).startsWith("run-as: package not debuggable:")) {
                    final List<String> dumpPackageInfo = IOUtils
                            .readLines(device.executeShell("dumpsys package " + packageName), Charset.defaultCharset());
                    log.trace("Found package info {}", dumpPackageInfo);
                    // dataDir=/data/user/0/com.amazon.mShop.android.shopping
                    final String dataDirProperty = dumpPackageInfo.stream()
                            .filter(l -> l.contains("dataDir"))
                            .findFirst()
                            .orElseThrow(() -> new NoSuchElementException(
                                    String.format("Data dir not found {}", dumpPackageInfo)));
                    log.debug("Application data dir property is {}", dataDirProperty);
                    final String[] splittedProperty = dataDirProperty.split("=");
                    if (splittedProperty.length != 2) {
                        throw new IllegalArgumentException(
                                String.format("Error in data dir parsing {}", dataDirProperty));
                    }

                    final String applicationDataPath = StringUtils.trim(splittedProperty[1]);
                    log.debug("Application data path is {}", applicationDataPath);
                    paths.put(packageName, applicationDataPath);
                    log.debug("Found in allowed device [{}] package [{}] with path [{}]",
                            device.getSerial(),
                            packageName,
                            applicationDataPath);
                    insertPackageStatement.setString(1, device.getSerial());
                    insertPackageStatement.setString(2, packageName);
                    insertPackageStatement.setString(3, applicationDataPath);
                    insertPackageStatement.executeUpdate();
                }
            }

        } catch (final Exception e) {
            log.error("Fetching packages from device with serial [{}] get error [{}]", device.getSerial(), e);
        }
        return paths;
    }
}
