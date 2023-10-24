variable "fill_bucket_lambda_arn" {
  description = "The lambda arn for the FillS3Bucket step"
  type = string
}

variable "process_bucket_key_lambda_arn" {
  description = "The lambda arn for the ProcessS3Key step"
  type = string
}