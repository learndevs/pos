# Database (installed separately)

The installers ship the app, a Java runtime and the PostgreSQL JDBC driver (inside the jar).
PostgreSQL itself runs as its own service, so it can be upgraded, backed up and monitored on its own.

## Windows till / admin PC
1. Install PostgreSQL 17 (EDB installer): it registers the `postgresql-x64-17` Windows service, auto-start.
   Keep the default port 5432. On a till, listening on localhost only is enough.
2. Create the role and databases: `psql -U postgres -f create-databases.sql` (edit the password first).
3. Start Retail POS once. It writes `%APPDATA%\Retail POS\database.json` and stops with a message.
   (Admin Server: `%APPDATA%\Retail POS Admin Server\database.json`.)
4. Fill it in and start again:
   ```json
   { "url": "jdbc:postgresql://127.0.0.1:5432/pos", "user": "pos_app", "password": "..." }
   ```
   Flyway creates or upgrades all tables on start.

The database can also be on another PC (`jdbc:postgresql://192.168.1.20:5432/pos`) or in Docker
(`packaging/admin/docker-compose.yml` runs one for the admin server).
