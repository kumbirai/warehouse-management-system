#!/bin/bash

# Script to drop and recreate wms_notification_db with proper schema-per-tenant setup
# This ensures the database follows the schema-per-tenant pattern required for MVP

# Database configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-secret}
DB_NAME="wms_notification_db"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to execute PostgreSQL command
execute_psql() {
    local database=$1
    local command=$2
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$database" -c "$command" 2>&1
}

# Function to check if database exists
database_exists() {
    local db_name=$1
    local result=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1 FROM pg_database WHERE datname = '$db_name';" 2>/dev/null | grep -c "1 row")
    [ "$result" -eq 1 ]
}

# Function to drop database
drop_database() {
    print_warn "Dropping database '$DB_NAME'..."
    
    # Terminate all connections to the database first
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" > /dev/null 2>&1
    
    local result=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;" 2>&1)
    
    if [ $? -eq 0 ]; then
        print_info "Database '$DB_NAME' dropped successfully."
        return 0
    else
        print_error "Failed to drop database '$DB_NAME': $result"
        return 1
    fi
}

# Function to create database
create_database() {
    print_info "Creating database '$DB_NAME'..."
    local result=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" 2>&1)
    
    if [ $? -eq 0 ]; then
        print_info "Database '$DB_NAME' created successfully."
        return 0
    else
        print_error "Failed to create database '$DB_NAME': $result"
        return 1
    fi
}

# Function to verify database is empty (only public schema)
verify_database_state() {
    print_info "Verifying database state..."
    
    # Check schemas (should only have public)
    local schemas=$(execute_psql "$DB_NAME" "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'pg_toast') ORDER BY schema_name;" 2>/dev/null | grep -v "schema_name" | grep -v "public" | grep -v "rows" | grep -v "^-" | grep -v "^$" | wc -l)
    
    if [ "$schemas" -eq 0 ]; then
        print_info "Database is clean (only public schema exists)."
        return 0
    else
        print_warn "Database has additional schemas. This is expected if tenant schemas exist."
        return 0
    fi
}

# Main execution
main() {
    print_info "Resetting notification database for schema-per-tenant pattern..."
    echo ""
    
    # Check if psql is available
    if ! command -v psql &> /dev/null; then
        print_error "psql command not found. Please install PostgreSQL client."
        exit 1
    fi
    
    # Test database connection
    print_info "Testing database connection to $DB_HOST:$DB_PORT..."
    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1;" > /dev/null 2>&1; then
        print_error "Failed to connect to PostgreSQL. Please check your connection settings."
        exit 1
    fi
    print_info "Database connection successful."
    echo ""
    
    # Drop database if it exists
    if database_exists "$DB_NAME"; then
        if ! drop_database; then
            print_error "Failed to drop database. Aborting."
            exit 1
        fi
    else
        print_warn "Database '$DB_NAME' does not exist. Skipping drop."
    fi
    
    echo ""
    
    # Create database
    if ! create_database; then
        print_error "Failed to create database. Aborting."
        exit 1
    fi
    
    echo ""
    
    # Verify database state
    verify_database_state
    
    echo ""
    print_info "Database reset complete!"
    print_info "Next steps:"
    print_info "  1. Start notification-service (Flyway will run migrations in public schema for validation)"
    print_info "  2. Create and activate tenants (tenant-service will create tenant schemas)"
    print_info "  3. Notification-service will create tables in tenant schemas when TenantSchemaCreatedEvent is received"
    print_info ""
    print_warn "Note: Tables will be created in tenant schemas, not public schema."
    print_warn "      The public schema will remain empty (as required for schema-per-tenant pattern)."
}

# Run main function
main "$@"

