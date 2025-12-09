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

variable "kafka_instance_type" {
  description = "Kafka instance type"
  type        = string
  default     = "kafka.m5.large"
}

variable "kafka_storage_size" {
  description = "Kafka storage size in GB"
  type        = number
  default     = 100
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "subnet_ids" {
  description = "Subnet IDs for Kafka cluster"
  type        = list(string)
}

variable "allowed_security_group_ids" {
  description = "Security group IDs allowed to access Kafka"
  type        = list(string)
}

resource "aws_msk_cluster" "wms_kafka" {
  cluster_name           = "${var.project_name}-${var.environment}-kafka"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = var.environment == "production" ? 3 : 1

  broker_node_group_info {
    instance_type   = var.kafka_instance_type
    client_subnets  = var.subnet_ids
    security_groups = [aws_security_group.wms_kafka_sg.id]

    storage_info {
      ebs_storage_info {
        volume_size = var.kafka_storage_size
      }
    }
  }

  encryption_info {
    encryption_at_rest_kms_key_id = aws_kms_key.wms_kafka_key.arn
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.wms_kafka_logs.name
      }
    }
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-kafka"
    Environment = var.environment
  }
}

resource "aws_kms_key" "wms_kafka_key" {
  description             = "KMS key for MSK cluster encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true

  tags = {
    Name        = "${var.project_name}-${var.environment}-kafka-key"
    Environment = var.environment
  }
}

resource "aws_kms_alias" "wms_kafka_key_alias" {
  name          = "alias/${var.project_name}-${var.environment}-kafka"
  target_key_id = aws_kms_key.wms_kafka_key.key_id
}

resource "aws_cloudwatch_log_group" "wms_kafka_logs" {
  name              = "/aws/msk/${var.project_name}-${var.environment}"
  retention_in_days = 7
}

resource "aws_security_group" "wms_kafka_sg" {
  name        = "${var.project_name}-${var.environment}-kafka-sg"
  description = "Security group for WMS Kafka cluster"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Kafka from allowed security groups"
    from_port       = 9092
    to_port         = 9098
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
    Name        = "${var.project_name}-${var.environment}-kafka-sg"
    Environment = var.environment
  }
}

output "kafka_bootstrap_brokers" {
  description = "Kafka bootstrap brokers"
  value       = aws_msk_cluster.wms_kafka.bootstrap_brokers
  sensitive   = true
}

output "kafka_bootstrap_brokers_tls" {
  description = "Kafka bootstrap brokers TLS"
  value       = aws_msk_cluster.wms_kafka.bootstrap_brokers_tls
  sensitive   = true
}

output "kafka_zookeeper_connect_string" {
  description = "Zookeeper connect string"
  value       = aws_msk_cluster.wms_kafka.zookeeper_connect_string
  sensitive   = true
}

