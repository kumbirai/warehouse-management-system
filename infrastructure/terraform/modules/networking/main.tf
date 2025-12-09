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

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones"
  type        = list(string)
}

resource "aws_vpc" "wms_vpc" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "${var.project_name}-${var.environment}-vpc"
    Environment = var.environment
  }
}

resource "aws_internet_gateway" "wms_igw" {
  vpc_id = aws_vpc.wms_vpc.id

  tags = {
    Name        = "${var.project_name}-${var.environment}-igw"
    Environment = var.environment
  }
}

resource "aws_subnet" "wms_public_subnets" {
  count                   = length(var.availability_zones)
  vpc_id                  = aws_vpc.wms_vpc.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name        = "${var.project_name}-${var.environment}-public-subnet-${count.index + 1}"
    Environment = var.environment
    Type        = "public"
  }
}

resource "aws_subnet" "wms_private_subnets" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.wms_vpc.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name        = "${var.project_name}-${var.environment}-private-subnet-${count.index + 1}"
    Environment = var.environment
    Type        = "private"
  }
}

resource "aws_eip" "wms_nat_eips" {
  count  = length(var.availability_zones)
  domain = "vpc"

  tags = {
    Name        = "${var.project_name}-${var.environment}-nat-eip-${count.index + 1}"
    Environment = var.environment
  }
}

resource "aws_nat_gateway" "wms_nat_gateways" {
  count         = length(var.availability_zones)
  allocation_id = aws_eip.wms_nat_eips[count.index].id
  subnet_id     = aws_subnet.wms_public_subnets[count.index].id

  tags = {
    Name        = "${var.project_name}-${var.environment}-nat-gateway-${count.index + 1}"
    Environment = var.environment
  }

  depends_on = [aws_internet_gateway.wms_igw]
}

resource "aws_route_table" "wms_public_rt" {
  vpc_id = aws_vpc.wms_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.wms_igw.id
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-public-rt"
    Environment = var.environment
  }
}

resource "aws_route_table_association" "wms_public_rta" {
  count          = length(aws_subnet.wms_public_subnets)
  subnet_id      = aws_subnet.wms_public_subnets[count.index].id
  route_table_id = aws_route_table.wms_public_rt.id
}

resource "aws_route_table" "wms_private_rt" {
  count  = length(var.availability_zones)
  vpc_id = aws_vpc.wms_vpc.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.wms_nat_gateways[count.index].id
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-private-rt-${count.index + 1}"
    Environment = var.environment
  }
}

resource "aws_route_table_association" "wms_private_rta" {
  count          = length(aws_subnet.wms_private_subnets)
  subnet_id      = aws_subnet.wms_private_subnets[count.index].id
  route_table_id = aws_route_table.wms_private_rt[count.index].id
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.wms_vpc.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = aws_subnet.wms_public_subnets[*].id
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = aws_subnet.wms_private_subnets[*].id
}

output "nat_gateway_ids" {
  description = "NAT Gateway IDs"
  value       = aws_nat_gateway.wms_nat_gateways[*].id
}

