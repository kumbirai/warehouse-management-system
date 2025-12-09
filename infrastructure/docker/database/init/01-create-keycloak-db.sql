-- Create keycloak database for Keycloak
CREATE
DATABASE keycloak;

-- Grant all privileges to postgres user on keycloak database
GRANT ALL PRIVILEGES ON DATABASE
keycloak TO postgres;
