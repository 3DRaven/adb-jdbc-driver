drop table if exists devices;
create table adb_sql_driver_devices (id INTEGER PRIMARY KEY AUTOINCREMENT, name string NOT NULL, serial string NOT NULL);
drop table if exists packages;
create table adb_sql_driver_packages (id INTEGER PRIMARY KEY AUTOINCREMENT, name string NOT NULL, path string NOT NULL);
drop table if exists files;
create table adb_sql_driver_files (id INTEGER PRIMARY KEY AUTOINCREMENT, device string NOT NULL, package string NOT NULL, "path" string NOT NULL, name string NOT NULL, absolute_path string NOT NULL);