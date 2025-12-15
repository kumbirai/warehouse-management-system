# CSV Template Files

This directory contains CSV template files for data ingestion into the Warehouse Management System.

## Available Templates

1. **`product_master_data_template.csv`** - Product master data template
2. **`stock_consignment_template.csv`** - Stock consignment template
3. **`picking_list_template.csv`** - Picking list template

## Usage

### Step 1: Download Template

Download the appropriate template file for your data type.

### Step 2: Fill in Data

1. Open the template in Excel or a text editor
2. Replace the example rows with your actual data
3. Ensure all required columns are populated
4. Follow the data format specifications (see [CSV Format Specification](../CSV_Format_Specification.md))

### Step 3: Validate Data

Before uploading, ensure:

- All required columns are present
- Data types match specifications
- Required fields are not empty
- Dates are in ISO 8601 format
- Quantities are positive numbers

### Step 4: Upload CSV

Upload the CSV file via:

- **REST API**: `POST /api/v1/{service}/upload-csv`
- **Web UI**: Use the file upload interface

## File Naming Convention

Use the following naming convention for uploaded files:

- **Products**: `products_YYYYMMDD_HHMMSS.csv`
- **Consignments**: `consignments_YYYYMMDD_HHMMSS.csv`
- **Picking Lists**: `picking_lists_YYYYMMDD_HHMMSS.csv`

Example: `products_20251115_103000.csv`

## Column Requirements

### Required Columns

Each template includes all required columns. Do not remove or rename columns.

### Optional Columns

Optional columns can be:

- Left empty (blank values)
- Removed if not needed (but recommended to keep for future use)

## Data Format Guidelines

### Dates

- Format: `YYYY-MM-DD` (e.g., `2025-11-15`)
- With time: `YYYY-MM-DDTHH:mm:ssZ` (e.g., `2025-11-15T10:30:00Z`)

### Numbers

- Use decimal point (`.`)
- No thousands separator
- Example: `100.50` (not `100,50` or `100.5`)

### Text

- UTF-8 encoding
- Use quotes (`"`) if text contains commas
- Example: `"123 Main St, Johannesburg"`

### Boolean

- Use `true` or `false` (case-insensitive)
- Alternative: `1` (true) or `0` (false)

## Examples

See the template files for example data rows. Each template includes 2-4 example rows demonstrating the correct format.

## Support

For questions or issues:

- **Documentation**: See [CSV Format Specification](../CSV_Format_Specification.md)
- **D365 Mapping**: See [D365 Entity Mapping Guide](../D365_Entity_Mapping_Guide.md)
- **Technical Support**: Contact system administrator

