variable "function_name" {
  description = "the function name of the lambda"
  type = string
}

variable "main_class" {
  description = "The main class (fully qualified) having the @SpringBootApplication annotation"
  type = string
}

variable "lambda_system_properties" {
  description = "lambda system properties"
  type        = object({
    logging_level = string
    spring_active_profile = string
  })
}

variable "lambda_dist_path" {
  description = "Path of the distribution zip of the lambda"
  type        = string
}

variable "lambda_dist_bucket" {
  description = "Bucket for the distribution zip of the lambda"
  type        = string
}

variable "lambda_dist_bucket_key" {
  description = "Bucket key for the distribution zip of the lambda"
  type        = string
}