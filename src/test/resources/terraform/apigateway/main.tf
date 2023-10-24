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
  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method   = "POST"
  status_code   = 200
}

resource "aws_api_gateway_method_response" "run_sf_response_fail" {
  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method   = "POST"
  status_code   = 409
}

resource "aws_api_gateway_integration" "run_sf_integration" {
  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method   = "POST"
  type                    = "AWS"
  integration_http_method = "POST"
  uri                     = var.state_machine_arn
  passthrough_behavior    = "WHEN_NO_MATCH"
}

resource "aws_api_gateway_integration_response" "api_integration_response_successful" {
  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method = "POST"
  status_code = 200
}

resource "aws_api_gateway_integration_response" "api_integration_response_fail" {
  rest_api_id   = aws_api_gateway_rest_api.demo_rest_api.id
  resource_id   = aws_api_gateway_resource.run_sf_resource.id
  http_method = "POST"
  status_code = 409
  selection_pattern = "The workflow failed"
}
























































module "create_user" {
  source = "../webapp_apigw_integration"
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  resource_id = aws_api_gateway_resource.webapp_users_resource.id
  http_method = "POST"
  authorization = "NONE"
  authorizer_id = null
  lambda_invocation_arn = var.customer_lambda_info.invoke_arn
  http_successful_status_code = "200"
  request_template_for_body = "$input.json('$')"
  spring_cloud_function_definition_header_value = "createUser"
  http_fail_status_codes = [
    {
      status_code = "400"
      selection_pattern = "User already exists"
    }
  ]
}

# ********* POST /login
resource "aws_api_gateway_resource" "webapp_login_resource" {
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  parent_id   = aws_api_gateway_rest_api.webapp_rest_api.root_resource_id
  path_part   = "login"
}

module "user_login" {
  source = "../webapp_apigw_integration"
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  resource_id = aws_api_gateway_resource.webapp_login_resource.id
  http_method = "POST"
  authorization = "NONE"
  authorizer_id = null
  lambda_invocation_arn = var.customer_lambda_info.invoke_arn
  http_successful_status_code = "200"
  request_template_for_body = "$input.json('$')"
  spring_cloud_function_definition_header_value = "loginUser"
  http_fail_status_codes = [
    {
      status_code = "401"
      selection_pattern = "The password for the user .* is wrong"
    }
  ]
}

# ********* GET /users/me
resource "aws_api_gateway_resource" "webapp_users_me_resource" {
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  parent_id   = aws_api_gateway_resource.webapp_users_resource.id
  path_part   = "me"
}

module "get_user" {
  source = "../webapp_apigw_integration"
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  resource_id = aws_api_gateway_resource.webapp_users_me_resource.id
  http_method = "GET"
  authorization = "CUSTOM"
  authorizer_id = aws_api_gateway_authorizer.custom_authorizer.id
  lambda_invocation_arn = var.customer_lambda_info.invoke_arn
  http_successful_status_code = "200"
  request_template_for_body = <<-EOT
    {
      "userId": "$context.authorizer.dbId"
    }
    EOT
  spring_cloud_function_definition_header_value = "getUser"
  http_fail_status_codes = [
    {
      status_code = "404"
      selection_pattern = "User with id \\d+ does not exist"
    }
  ]
}

# ********* DELETE /users/me

module "delete_user" {
  source = "../webapp_apigw_integration"
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  resource_id = aws_api_gateway_resource.webapp_users_me_resource.id
  http_method = "DELETE"
  authorization = "CUSTOM"
  authorizer_id = aws_api_gateway_authorizer.custom_authorizer.id
  lambda_invocation_arn = var.customer_lambda_info.invoke_arn
  http_successful_status_code = "200"
  request_template_for_body = <<-EOT
    {
      "userId": "$context.authorizer.dbId",
      "username": "$context.authorizer.username"
    }
    EOT
  spring_cloud_function_definition_header_value = "deleteUser"
  http_fail_status_codes = [
    {
      status_code = "404"
      selection_pattern = "User with id \\d+ does not exist"
    }
  ]
}

# ********* POST /users/me/subscriptions/{shopId}

resource "aws_api_gateway_resource" "webapp_users_me_subscriptions_resource" {
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  parent_id   = aws_api_gateway_resource.webapp_users_me_resource.id
  path_part   = "subscriptions"
}

resource "aws_api_gateway_resource" "webapp_users_me_subscriptions_with_shopId_resource" {
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  parent_id   = aws_api_gateway_resource.webapp_users_me_subscriptions_resource.id
  path_part   = "{shopId}"
}

module "subscribe_to_shop" {
  source = "../webapp_apigw_integration"
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  resource_id = aws_api_gateway_resource.webapp_users_me_subscriptions_with_shopId_resource.id
  http_method = "POST"
  authorization = "CUSTOM"
  authorizer_id = aws_api_gateway_authorizer.custom_authorizer.id
  lambda_invocation_arn = var.customer_lambda_info.invoke_arn
  http_successful_status_code = "200"
  request_template_for_body = <<-EOT
    {
      "username": "$context.authorizer.username",
      "userId": "$context.authorizer.dbId",
      "shopId": "$input.params('shopId')"
    }
    EOT
  spring_cloud_function_definition_header_value = "subscribeToShop"
  http_fail_status_codes = [
    {
      status_code = "404"
      selection_pattern = "(Shop|User) with id \\d+ does not exist"
    }
  ]
}

## ************************ Shop API ************************

# ********* DELETE /shops/{shopId}
resource "aws_api_gateway_resource" "webapp_shops_resource" {
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  parent_id   = aws_api_gateway_rest_api.webapp_rest_api.root_resource_id
  path_part   = "shops"
}

resource "aws_api_gateway_resource" "webapp_shops_with_shopId_resource" {
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  parent_id   = aws_api_gateway_resource.webapp_shops_resource.id
  path_part   = "{shopId}"
}

module "delete_shop" {
  source = "../webapp_apigw_integration"
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  resource_id = aws_api_gateway_resource.webapp_shops_with_shopId_resource.id
  http_method = "DELETE"
  authorization = "CUSTOM"
  authorizer_id = aws_api_gateway_authorizer.custom_authorizer.id
  lambda_invocation_arn = var.shop_lambda_info.invoke_arn
  http_successful_status_code = "200"
  request_template_for_body = <<-EOT
    {
      "userId": "$context.authorizer.dbId",
      "shopId": "$input.params('shopId')"
    }
    EOT
  spring_cloud_function_definition_header_value = "deleteShop"
  http_fail_status_codes = [
    {
      status_code = "404"
      selection_pattern = "Shop with id \\d+ does not exist"
    },
    {
      status_code = "403"
      selection_pattern = "User with id \\d+ is not the owner of shop \\d+"
    }
  ]
}

# ********* POST /shops/{shopId}/messages

resource "aws_api_gateway_resource" "webapp_shops_with_shopId_message_resource" {
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  parent_id   = aws_api_gateway_resource.webapp_shops_with_shopId_resource.id
  path_part   = "messages"
}

module "publish_message" {
  source = "../webapp_apigw_integration"
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  resource_id = aws_api_gateway_resource.webapp_shops_with_shopId_message_resource.id
  http_method = "POST"
  authorization = "CUSTOM"
  authorizer_id = aws_api_gateway_authorizer.custom_authorizer.id
  lambda_invocation_arn = var.shop_lambda_info.invoke_arn
  http_successful_status_code = "200"
  request_template_for_body = <<-EOT
    {
      "userId": "$context.authorizer.dbId",
      "shopId": "$input.params('shopId')",
      "message": "$input.path('$.message')"
    }
    EOT
  spring_cloud_function_definition_header_value = "publishMessage"
  http_fail_status_codes = [
    {
      status_code = "404"
      selection_pattern = "Shop with id \\d+ does not exist"
    },
    {
      status_code = "403"
      selection_pattern = "User with id \\d+ is not the owner of shop \\d+"
    }
  ]
}

## ************************ Admin API ************************

# ********* POST /shops

module "create_shop" {
  source = "../webapp_apigw_integration"
  rest_api_id = aws_api_gateway_rest_api.webapp_rest_api.id
  resource_id = aws_api_gateway_resource.webapp_shops_resource.id
  http_method = "POST"
  authorization = "CUSTOM"
  authorizer_id = aws_api_gateway_authorizer.custom_authorizer.id
  lambda_invocation_arn = var.admin_lambda_info.invoke_arn
  http_successful_status_code = "200"
  request_template_for_body = "$input.json('$')"
  spring_cloud_function_definition_header_value = "createShop"
  http_fail_status_codes = [
    {
      status_code = "404"
      selection_pattern = "User with id \\d+ does not exist"
    }
  ]
}