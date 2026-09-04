-- Creates one database per service so each owns its schema independently.
-- Runs automatically the first time the postgres container is initialised.
CREATE DATABASE payflow_orchestrator;
CREATE DATABASE payflow_ledger;
CREATE DATABASE payflow_refund;
CREATE DATABASE payflow_payout;
CREATE DATABASE payflow_gateway;
