-- Create databases for all services
CREATE DATABASE userdb;
CREATE DATABASE productdb;
CREATE DATABASE inventorydb;
CREATE DATABASE orderdb;
CREATE DATABASE paymentdb;
-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE userdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE productdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE inventorydb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO postgres;