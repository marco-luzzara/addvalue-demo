resource "aws_sfn_state_machine" "demo_state_machine" {
  name = "demo-state-machine"
  role_arn = "arn:aws:iam::000000000000:role/faking-role"
  definition = <<EOF
    {
      "Comment": "Demo workflow",
      "StartAt": "CreateS3Bucket",
      "States": {
        "CreateS3Bucket": {
          "Type": "Task",
          "Parameters": {
            "Bucket.$": "$$.Execution.Input.bucketName"
          },
          "Resource": "arn:aws:states:::aws-sdk:s3:createBucket",
          "Next": "FillS3Bucket"
        },
        "FillS3Bucket": {
          "Type": "Task",
          "Resource": "${var.fill_bucket_lambda_arn}",
          "Parameters": {
            "bucketName.$": "$$.Execution.Input.bucketName",
            "keyCount.$": "$$.Execution.Input.keyCount"
          },
          "Retry": [
            {
              "ErrorEquals": [
                "Lambda.ServiceException",
                "Lambda.AWSLambdaException",
                "Lambda.SdkClientException",
                "Lambda.TooManyRequestsException"
              ],
              "IntervalSeconds": 1,
              "MaxAttempts": 3,
              "BackoffRate": 2
            }
          ],
          "Next": "ProcessS3Keys",
          "ResultSelector": {
            "bucketName.$": "$$.Execution.Input.bucketName"
          }
        },
        "ProcessS3Keys": {
          "Type": "Map",
          "ItemProcessor": {
            "ProcessorConfig": {
              "Mode": "DISTRIBUTED",
              "ExecutionType": "STANDARD"
            },
            "StartAt": "ProcessS3Key",
            "States": {
              "ProcessS3Key": {
                "Type": "Task",
                "Resource": "${var.process_bucket_key_lambda_arn}",
                "OutputPath": "$.Payload",
                "Parameters": {
                  "Payload.$": "$"
                },
                "Retry": [
                  {
                    "ErrorEquals": [
                      "Lambda.ServiceException",
                      "Lambda.AWSLambdaException",
                      "Lambda.SdkClientException",
                      "Lambda.TooManyRequestsException"
                    ],
                    "IntervalSeconds": 1,
                    "MaxAttempts": 3,
                    "BackoffRate": 2
                  }
                ],
                "End": true
              }
            }
          },
          "ItemReader": {
            "Resource": "arn:aws:states:::s3:listObjectsV2",
            "Parameters": {
              "Bucket.$": "$.bucketName"
            }
          },
          "MaxConcurrency": 1000,
          "End": true
        }
      }
    }
  EOF
}