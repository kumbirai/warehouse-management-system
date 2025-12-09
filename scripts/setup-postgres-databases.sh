#!/bin/bash

# Database configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-secret}

# List of all databases
DATABASES=(
    "wms_stock_management_db"
    "wms_location_management_db"
    "wms_product_db"
    "wms_picking_db"
    "wms_returns_db"
    "wms_reconciliation_db"
    "wms_integration_db"
    "wms_tenant_db"
    "wms_user_db"
    "wms_notification_db"
)

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
    local command=$1
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "$command" 2>&1
}

# Function to check if database exists
database_exists() {
    local db_name=$1
    local result=$(execute_psql "SELECT 1 FROM pg_database WHERE datname = '$db_name';" | grep -c "1 row")
    [ "$result" -eq 1 ]
}

# Function to create a database
create_database() {
    local db_name=$1
    if database_exists "$db_name"; then
        print_warn "Database '$db_name' already exists. Skipping creation."
        return 0
    fi
    
    print_info "Creating database '$db_name'..."
    local result=$(execute_psql "CREATE DATABASE $db_name;")
    
    if [ $? -eq 0 ]; then
        print_info "Database '$db_name' created successfully."
        return 0
    else
        print_error "Failed to create database '$db_name': $result"
        return 1
    fi
}

# Function to drop a database
drop_database() {
    local db_name=$1
    if ! database_exists "$db_name"; then
        print_warn "Database '$db_name' does not exist. Skipping drop."
        return 0
    fi
    
    print_warn "Dropping database '$db_name'..."
    # Terminate all connections to the database first
    execute_psql "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$db_name' AND pid <> pg_backend_pid();" > /dev/null 2>&1
    
    local result=$(execute_psql "DROP DATABASE $db_name;")
    
    if [ $? -eq 0 ]; then
        print_info "Database '$db_name' dropped successfully."
        return 0
    else
        print_error "Failed to drop database '$db_name': $result"
        return 1
    fi
}

# Function to create all databases
create_all_databases() {
    print_info "Creating all databases..."
    local failed=0
    for db in "${DATABASES[@]}"; do
        if ! create_database "$db"; then
            failed=$((failed + 1))
        fi
    done
    
    if [ $failed -eq 0 ]; then
        print_info "All databases created successfully."
        return 0
    else
        print_error "$failed database(s) failed to create."
        return 1
    fi
}

# Function to drop all databases
drop_all_databases() {
    print_warn "This will drop ALL databases. Are you sure? (yes/no)"
    read -r confirmation
    if [ "$confirmation" != "yes" ]; then
        print_info "Operation cancelled."
        return 0
    fi
    
    print_info "Dropping all databases..."
    local failed=0
    for db in "${DATABASES[@]}"; do
        if ! drop_database "$db"; then
            failed=$((failed + 1))
        fi
    done
    
    if [ $failed -eq 0 ]; then
        print_info "All databases dropped successfully."
        return 0
    else
        print_error "$failed database(s) failed to drop."
        return 1
    fi
}

# Function to list all databases
list_databases() {
    print_info "Listing all WMS databases:"
    for db in "${DATABASES[@]}"; do
        if database_exists "$db"; then
            echo -e "  ${GREEN}✓${NC} $db"
        else
            echo -e "  ${RED}✗${NC} $db"
        fi
    done
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [COMMAND] [DATABASE_NAME]"
    echo ""
    echo "Commands:"
    echo "  create-all          Create all databases"
    echo "  drop-all            Drop all databases (with confirmation)"
    echo "  create <db_name>    Create a specific database"
    echo "  drop <db_name>      Drop a specific database"
    echo "  list                List all databases and their status"
    echo "  help                Show this help message"
    echo ""
    echo "Available databases:"
    for db in "${DATABASES[@]}"; do
        echo "  - $db"
    done
    echo ""
    echo "Environment variables:"
    echo "  DB_HOST     - PostgreSQL host (default: localhost)"
    echo "  DB_PORT     - PostgreSQL port (default: 5432)"
    echo "  DB_USER     - PostgreSQL user (default: postgres)"
    echo "  DB_PASSWORD - PostgreSQL password (default: secret)"
    echo ""
    echo "Examples:"
    echo "  $0 create-all"
    echo "  $0 drop-all"
    echo "  $0 create wms_stock_management_db"
    echo "  $0 drop wms_user_db"
    echo "  $0 list"
}

# Function to validate database name
validate_database_name() {
    local db_name=$1
    for db in "${DATABASES[@]}"; do
        if [ "$db" == "$db_name" ]; then
            return 0
        fi
    done
    return 1
}

# Main script logic
main() {
    # Check if psql is available
    if ! command -v psql &> /dev/null; then
        print_error "psql command not found. Please install PostgreSQL client."
        exit 1
    fi
    
    # Test database connection
    print_info "Testing database connection to $DB_HOST:$DB_PORT..."
    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1;" > /dev/null 2>&1; then
        print_error "Failed to connect to PostgreSQL. Please check your connection settings."
        print_info "Host: $DB_HOST"
        print_info "Port: $DB_PORT"
        print_info "User: $DB_USER"
        exit 1
    fi
    print_info "Database connection successful."
    echo ""
    
    # Parse command
    case "${1:-help}" in
        create-all)
            create_all_databases
            ;;
        drop-all)
            drop_all_databases
            ;;
        create)
            if [ -z "$2" ]; then
                print_error "Database name is required for create command."
                show_usage
                exit 1
            fi
            if ! validate_database_name "$2"; then
                print_error "Invalid database name: $2"
                echo ""
                show_usage
                exit 1
            fi
            create_database "$2"
            ;;
        drop)
            if [ -z "$2" ]; then
                print_error "Database name is required for drop command."
                show_usage
                exit 1
            fi
            if ! validate_database_name "$2"; then
                print_error "Invalid database name: $2"
                echo ""
                show_usage
                exit 1
            fi
            drop_database "$2"
            ;;
        list)
            list_databases
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            print_error "Unknown command: $1"
            echo ""
            show_usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"

