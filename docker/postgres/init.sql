-- Run once on first container init (only if the data directory is empty).
-- Creates the Keycloak database alongside the application database
-- (ophthacloud_db is created automatically via POSTGRES_DB).
CREATE DATABASE keycloak_db;
