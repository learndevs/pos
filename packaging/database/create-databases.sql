-- Run once as the postgres superuser after installing PostgreSQL 17:
--   psql -U postgres -f create-databases.sql
-- Change the password, then put the same values in database.json on each PC.
CREATE ROLE pos_app LOGIN PASSWORD 'change-me';
CREATE DATABASE pos       OWNER pos_app ENCODING 'UTF8';   -- till PC
CREATE DATABASE pos_admin OWNER pos_app ENCODING 'UTF8';   -- admin server PC (skip on tills)
