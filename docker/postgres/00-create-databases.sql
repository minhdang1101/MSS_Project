-- Database-per-service: one database per microservice (run once on a fresh PostgreSQL)
CREATE DATABASE catalog_db;
CREATE DATABASE member_db;
CREATE DATABASE loan_db;
CREATE DATABASE fine_db;
CREATE DATABASE notification_db;
