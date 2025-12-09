terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "wms"
}

variable "database_instance_class" {
  description = "Database instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "database_allocated_storage" {
  description = "Allocated storage in GB"
  type        = number
  default     = 100
}

variable "database_name" {
  description = "Database name"
  type        = string
  default     = "wms_db"
}

variable "database_username" {
  description = "Database master username"
  type        = string
  default     = "postgres"
}

variable "vpc_id" {
  description = "VPC ID for database subnet group"
  type        = string
}

variable "subnet_ids" {
  description = "Subnet IDs for database subnet group"
  type        = list(string)
}

variable "allowed_security_group_ids" {
  description = "Security group IDs allowed to access database"
  type        = list(string)
}

resource "aws_db_subnet_group" "wms_db_subnet_group" {
  name       = "${var.project_name}-${var.environment}-db-subnet-group"
  subnet_ids = var.subnet_ids

  tags = {
    Name        = "${var.project_name}-${var.environment}-db-subnet-group"
    Environment = var.environment
  }
}

resource "aws_db_instance" "wms_database" {
  identifier             = "${var.project_name}-${var.environment}-db"
  engine                  = "postgres"
  engine_version          = "15.4"
  instance_class         = var.database_instance_class
  allocated_storage       = var.database_allocated_storage
  max_allocated_storage   = 500
  storage_type            = "gp3"
  storage_encrypted       = true
  db_name                 = var.database_name
  username                = var.database_username
  password                = var.database_password
  manage_master_user_password = false
  db_subnet_group_name    = aws_db_subnet_group.wms_db_subnet_group.name
  vpc_security_group_ids   = [aws_security_group.wms_db_sg.id]
  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"
  multi_az               = var.environment == "production" ? true : false
  skip_final_snapshot    = var.environment != "production"
  final_snapshot_identifier = var.environment == "production" ? "${var.project_name}-${var.environment}-final-snapshot" : null
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  performance_insights_enabled = true
  deletion_protection    = var.environment == "production"

  tags = {
    Name        = "${var.project_name}-${var.environment}-database"
    Environment = var.environment
  }
}

resource "aws_security_group" "wms_db_sg" {
  name        = "${var.project_name}-${var.environment}-db-sg"
  description = "Security group for WMS database"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PostgreSQL from allowed security groups"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-db-sg"
    Environment = var.environment
  }
}

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

