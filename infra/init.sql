-- Initialization script for SettleUp databases.
-- When the PostgreSQL container starts, it will execute this script
-- to create the databases for each microservice.  If the databases
-- already exist, these statements will do nothing.

-- Create the required databases only if they don't already exist.
-- We cannot run CREATE DATABASE inside a transaction block, so we use
-- a psql meta-command with \gexec to evaluate dynamic SQL conditionally.

-- Create membershipdb if it does not exist
SELECT 'CREATE DATABASE membershipdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'membershipdb')\gexec

-- Create expensedb if it does not exist
SELECT 'CREATE DATABASE expensedb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'expensedb')\gexec

-- Create settlementdb if it does not exist
SELECT 'CREATE DATABASE settlementdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'settlementdb')\gexec
