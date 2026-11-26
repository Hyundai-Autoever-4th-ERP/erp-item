CREATE ROLE item_app LOGIN PASSWORD 'item_local';
CREATE DATABASE item_db OWNER item_app;
REVOKE CONNECT ON DATABASE item_db FROM PUBLIC;

\connect item_db
SET ROLE item_app;
\i /opt/erp/master.sql
