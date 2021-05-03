drop table if exists devices;
create table devices (id INTEGER PRIMARY KEY AUTOINCREMENT, name string NOT NULL, serial string NOT NULL);
drop table if exists packages;
create table packages (id INTEGER PRIMARY KEY AUTOINCREMENT, device string NOT NULL, name string NOT NULL, path string NOT NULL);
drop table if exists files;
create table files (id INTEGER PRIMARY KEY AUTOINCREMENT, device string NOT NULL, package string NOT NULL, path string NOT NULL, name string NOT NULL, absolute_path string NOT NULL);