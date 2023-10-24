terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.17.0"
    }
  }

  required_version = ">= 1.2.0"
}

# provider in Localstack is overridden by provider_override.tf file because the
# endpoints must be manually set
provider "aws" {
  region = var.aws_region
}

module "fill_lambda" {
  source = "./lambda"

  lambda_dist_bucket = var.fill_lambda_dist_bucket
  lambda_dist_bucket_key = var.fill_lambda_dist_bucket_key
  lambda_dist_path = var.fill_lambda_dist_path
  lambda_system_properties = {
    logging_level = "INFO"
    spring_active_profile = var.fill_lambda_spring_active_profile
  }
  function_name = "fillS3BucketLambda"
  main_class = "it.addvalue.demo.Application"
}

module "process_lambda" {
  source = "./lambda"

  lambda_dist_bucket = var.process_lambda_dist_bucket
  lambda_dist_bucket_key = var.process_lambda_dist_bucket_key
  lambda_dist_path = var.process_lambda_dist_path
  lambda_system_properties = {
    logging_level = "INFO"
    spring_active_profile = var.process_lambda_spring_active_profile
  }
  function_name = "processS3BucketKeysLambda"
  main_class = "it.addvalue.demo.Application"
}

module "step_function_workflow" {
  source = "./stepfunctions"
  fill_bucket_lambda_arn = module.fill_lambda.lambda_arn
  process_bucket_key_lambda_arn = module.process_lambda.lambda_arn
}

module "apigateway" {
  source = "./apigateway"

  state_machine_arn = module.step_function_workflow.state_machine_arn
}

resource "aws_api_gateway_deployment" "apigw_deployment" {
  rest_api_id = module.apigateway.demo_rest_api_id
  stage_name  = "demo"
}