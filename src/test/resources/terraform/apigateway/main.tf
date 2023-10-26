resource "aws_api_gateway_rest_api" "demo_rest_api" {
  name = "webapp-api"
}

# ********* POST /run_sf
resource "aws_api_gateway_resource" "run_sf_resource" {
  rest_api_id = aws_api_gateway_rest_api.demo_rest_api.id
  parent_id   = aws_api_gateway_rest_api.demo_rest_api.root_resource_id
  path_part   = "run_sf"
}

resource "aws_api_gateway_method" "run_sf_method" {
  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method_response" "run_sf_response_successful" {
  depends_on = [aws_api_gateway_method.run_sf_method]

  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method   = "POST"
  status_code   = 200
}

resource "aws_api_gateway_method_response" "run_sf_response_fail" {
  depends_on = [aws_api_gateway_method.run_sf_method]

  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method   = "POST"
  status_code   = 409
}

resource "aws_api_gateway_integration" "run_sf_integration" {
  depends_on = [aws_api_gateway_method.run_sf_method]

  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method   = "POST"
  type                    = "AWS"
  integration_http_method = "POST"
  uri                     = "arn:aws:apigateway:${var.aws_region}:states:action/StartSyncExecution"
  passthrough_behavior    = "WHEN_NO_MATCH"
  request_templates = {
    "application/json" = <<-EOT
    {
      "input": "$util.escapeJavaScript($input.json('$'))",
      "stateMachineArn": "${var.state_machine_arn}"
    }
    EOT
  }
}

resource "aws_api_gateway_integration_response" "api_integration_response_successful" {
  depends_on = [aws_api_gateway_integration.run_sf_integration]

  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method = "POST"
  status_code = 200

  response_templates = {
    "application/json" = <<-EOT
    $util.parseJson($input.path('$.output'))
    EOT
  }
}

resource "aws_api_gateway_integration_response" "api_integration_response_fail" {
  depends_on = [aws_api_gateway_integration.run_sf_integration]

  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method = "POST"
  status_code = 409
  selection_pattern = "The workflow failed"
}
