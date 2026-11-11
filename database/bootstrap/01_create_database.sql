-- Run ONCE as administrator connected to postgres, with autocommit enabled.
-- CREATE DATABASE cannot run inside a transaction. If it already exists, stop;
-- do not drop/recreate it. This file never targets omni_connect.
CREATE DATABASE whatsapp_connect_ai OWNER wc_owner ENCODING 'UTF8' TEMPLATE template0;
REVOKE CONNECT ON DATABASE whatsapp_connect_ai FROM PUBLIC;
GRANT CONNECT ON DATABASE whatsapp_connect_ai TO wc_owner, wc_auth, wc_app, wc_worker;
