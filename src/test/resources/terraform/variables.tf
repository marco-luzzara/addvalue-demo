// ******************************** Fill Lambda

variable "fill_lambda_spring_active_profile" {
  description = "Spring active profile for the fill lambda"
  type        = string
}

variable "fill_lambda_dist_path" {
  description = "Path of the distribution zip of the fill lambda"
  type        = string
}

variable "fill_lambda_dist_bucket" {
  description = "Bucket for the distribution zip of the fill lambda"
  type        = string
  default = "lambda-dist-bucket"
}

variable "fill_lambda_dist_bucket_key" {
  description = "Bucket key for the distribution zip of the fill lambda"
  type        = string
  default = "general_lambda.zip"
}

// ******************************** Process Lambda Variables

variable "process_lambda_spring_active_profile" {
  description = "Spring active profile for the process lambda"
  type        = string
}

variable "process_lambda_dist_path" {
  description = "Path of the distribution zip of the process lambda"
  type        = string
}

variable "process_lambda_dist_bucket" {
  description = "Bucket for the distribution zip of the process lambda"
  type        = string
  default = "lambda-dist-bucket"
}

variable "process_lambda_dist_bucket_key" {
  description = "Bucket key for the distribution zip of the process lambda"
  type        = string
  default = "general_lambda.zip"
}

// **********************************

variable "aws_region" {
  description = "AWS region"
  type        = string
  default = "us-east-1"
}
