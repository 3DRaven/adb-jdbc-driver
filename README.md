# AdbJdbcDriver
JDBC driver for connecting to android devices and get all db information and other from debugable application
This driver shoud be used as Android Studio "Database Inspector" functionality for Visual Studio code and other IDEs.
You can use prefered sql connection plugin in any IDE and connect by this driver to your device.
# Usage:

You need started adb server

```adb start-server```

## Variant one:

Just create jdbc connection with this driver and
jdbc url string as 
```jdbc:adb://*/*```

Driver class name: 
```com.anyqn.adbjdbc.AdbJdbcDriver```
Need set Unauthorized access to datasource

This type of connection will read files, packages and list of devices to tables. So, you can find database file for your application.

## Variant two:
When you get information about device just connect with this driver and jdbc url string as:
```jdbc:adb://SM-G973F/com.anyqn.amhere/databases/main_database.db```

`SM-G973F` - your device name or serial
`com.anyqn.amhere` - your application package name
`/databases/main_database.db` - relative path to your db in application data directory

And you can look at db from your application

Parameters in jdbc url:
`applicationDataRootPath` default `/data/data` but you can set your own default path to data of your application
`initialDelay` in ms time how we need to wait before start scan application db from android device
`period` in ms time between reload data from device application db

as example
```jdbc:adb://SM-G973F/com.anyqn.amhere/databases/main_database.db?applicationDataRootPath=/data/data&initialDelay=0&period=5000```

# Maven
```
<dependency>
    <groupId>com.anyqn.lib</groupId>
    <artifactId>adb-jdbc-driver</artifactId>
    <version>0.1</version>
</dependency>
```
# Testing

Only on my Samsung S10 now, but I will test this driver on pair of other devices. You are welcome to enjoy to testing :)

# License

Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/
