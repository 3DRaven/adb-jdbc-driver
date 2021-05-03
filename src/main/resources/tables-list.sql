SELECT
    name
FROM
    android_db.sqlite_master
WHERE
    type ='table' AND
    name NOT LIKE 'sqlite_%';