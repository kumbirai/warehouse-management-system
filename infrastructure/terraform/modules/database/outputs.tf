output "database_endpoint" {
  description = "Database endpoint"
  value       = aws_db_instance.wms_database.endpoint
  sensitive   = true
}

output "database_port" {
  description = "Database port"
  value       = aws_db_instance.wms_database.port
}

output "database_name" {
  description = "Database name"
  value       = aws_db_instance.wms_database.db_name
}

output "database_security_group_id" {
  description = "Database security group ID"
  value       = aws_security_group.wms_db_sg.id
}

