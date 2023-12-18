# Addvalue Demo

This demo shows how to test a Step Functions workflow using Localstack.

To start a workflow execution locally, run: 
- `make start` to build the local infrastructure
- `make run_sf` to send an HTTP request to API Gateway, which is integrated with Step Functions. This triggers a workflow execution, whose response is returned in the HTTP response

### Tests

To run tests, run `gradle test -DIntegrationTestsEnabled=true`
