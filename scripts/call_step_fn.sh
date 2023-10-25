#!/usr/bin/env bash

# template: https://sharats.me/posts/shell-script-best-practices/

set -o errexit
set -o nounset
set -o pipefail
if [[ "${TRACE-0}" == "1" ]]; then
    set -o xtrace
fi

if [[ "${1-}" =~ ^-*h(elp)?$ ]]; then
    echo "Usage: sudo ./$(basename "$0")"
    echo "It sends an HTTP request to the endpoint integrated with the step functions"
    exit
fi

cd "$(dirname "$0")"

main() {
    APIGW_REST_API_ID="$( ./get_rest_api_id.sh )"
    # the bucketName is the current timestamp
    curl -X POST -d "{ \"bucketName\": \"$(date +"%s")\", \"keyCount\": 3 }" -H "Content-Type: application/json" \
        "http://localhost:4566/restapis/$APIGW_REST_API_ID/demo/_user_request_/run_sf"
}

main "$@"