# Qlin-Me

## Introduction
This API provides endpoints to upload validate and check status of a batch.

A visual **OpenAPI** documentation is available at: https://qlin-me.cqgc.hsj.rtss.qc.ca

## Step by step

Bellow are the most used endpoints a user may want to know about.
- authentication => will grant a `token` for user `email` + `password`
- upload metadata => validate and save a `metadata` by `batch_id`
- status of batch => summary of the current state of the batch by `batch_id`

### Authentication

In order to use the API the user has to get a `token` using the auth endpoint:

**Example**

```shell
curl --location 'https://qlin-me.cqgc.hsj.rtss.qc.ca/api/v1/auth/login?email=foo%40bar.com&password=<user_pwd>'
```

All specials characters in email or password have to be percent encoded: https://www.w3schools.com/tags/ref_urlencode.asp

**OpenAPI Doc. ref.** :
https://qlin-me.cqgc.hsj.rtss.qc.ca/#tag/Authentication/operation/authLoginByEmailPassword

### Upload metadata

Will create or update the `metadata` for the `batch_id` if valid, otherwise return all the errors to the user.

```shell
curl --location 'https://qlin-me.cqgc.hsj.rtss.qc.ca/api/v1/batch/<batch_id>' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "submissionSchema" : "CQGC_Germline",
  "analyses" : [...]
}'
```

**OpenAPI Doc. ref.** :
https://qlin-me.cqgc.hsj.rtss.qc.ca/#tag/Batch/operation/batchCreateUpdate

### Status of a batch

Will validate and return errors of both `metadata` and `cram, tsv, json, vcfs ...` files by `batch_id`.
```shell
curl --location 'https://qlin-me.cqgc.hsj.rtss.qc.ca/api/v1/batch/<batch_id>/status' \
--header 'Authorization: Bearer <token>'
```

**Note:** That endpoint also works with `metadata` JSON file deployed manually though `aws-cli` on **S3**.


**OpenAPI Doc. ref.** :
https://qlin-me.cqgc.hsj.rtss.qc.ca/#tag/Batch/operation/batchStatus
