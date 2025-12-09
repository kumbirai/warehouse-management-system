terraform {
  backend "s3" {
    bucket         = "wms-terraform-state-production"
    key            = "terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-state-lock-production"
  }
}

