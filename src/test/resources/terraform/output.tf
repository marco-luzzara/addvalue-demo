output "webapp_apigw_rest_api_id" {
  value = module.apigateway.demo_rest_api_id
}

output "webapp_apigw_stage_name" {
  value = var.apigateway_stage_name
}