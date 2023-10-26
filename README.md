# Automated Security Benchmarking Tool

## What is it?

Tool to test the security of website by making requests with malicious payloads and verifying that the request is blocked by the firewall/application

## How does it work?

### Request Type 1 - Generic Payload Test

Using the text files within the resources > GenericPayloads folder, the runner will iterate through each of the files and take each line in the file as a payload to test. Three requests are made with the payload in different locations, the query string, a header and the post data. This helps ensure that rules are applied across all parts of the program, and ran correctly in places like query strings where the payload should be decoded before testing.

### Request Type 2 - CVE Test

Sightly more complicated, and involving more work that just copy and pasting a file, the CVE tester has a separate file located at resources/CVEPayloads/payloads.csv where once an interesting/popular POC/IOC is found, the contents of the request (cve|method|path|query_string|headers|post_data|files|source) need to be added to the file, and the system should send a request with it against the test site.

### Report Generation

When both cycles mentioned above are done, a report will then be generated. There is an option in the Constants file to turn on/off mentioning payloads that were successfully blocked, to help reduce noise, plus there is a chart for a high level overview of how each file performed against the WAF.

## Test Environment

I created my own GCP enviroment, hosting a Node.JS server with a basic FE as a Google Cloud Run container, and put a load balancer in front of it which allowed me to apply a Cloud Armor policy with security rules attached. This is what the benchmarking tool has been tested against, with all Cloud Armor rulesets enabled and set to level three sensitivity.