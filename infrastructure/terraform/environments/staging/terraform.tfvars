environment = "staging"
project_name = "wms"
region = "us-east-1"

kubernetes_cluster_name = "wms-staging-cluster"
database_instance_type = "db.t3.medium"
kafka_instance_type = "kafka.m5.large"

availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]
vpc_cidr = "10.1.0.0/16"

