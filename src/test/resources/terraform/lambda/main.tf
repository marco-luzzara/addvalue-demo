resource "aws_s3_bucket" "lambda_bucket" {
  count = var.lambda_dist_bucket == "hot-reload" ? 0 : 1

  bucket = var.lambda_dist_bucket
}

resource "aws_s3_object" "lambda_distribution_zip" {
  count = var.lambda_dist_bucket == "hot-reload" ? 0 : 1

  bucket = aws_s3_bucket.lambda_bucket[count.index].bucket
  key    = var.lambda_dist_bucket_key
  source = var.lambda_dist_path
}

resource "aws_lambda_function" "api_lambda" {
  depends_on = [aws_s3_object.lambda_distribution_zip]
  function_name = var.function_name
  runtime      = "java17"
  handler      = "org.springframework.cloud.function.adapter.aws.FunctionInvoker"
  role         = 'arn:aws:iam::000000000000:role/faking-role'
  timeout      = 900

  environment {
    variables = {
      JAVA_TOOL_OPTIONS = <<EOT
        -DMAIN_CLASS=${var.main_class}
        -Dlogging.level.org.springframework=${var.lambda_system_properties.logging_level}
        -Dspring.profiles.active=${var.lambda_system_properties.spring_active_profile}
      EOT
    }
  }

  s3_bucket = var.lambda_dist_bucket
  s3_key = var.lambda_dist_bucket_key
}