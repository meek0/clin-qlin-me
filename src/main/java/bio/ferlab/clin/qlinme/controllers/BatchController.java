package bio.ferlab.clin.qlinme.controllers;

import bio.ferlab.clin.qlinme.Routes;
import bio.ferlab.clin.qlinme.Utils;
import bio.ferlab.clin.qlinme.cients.S3Client;
import bio.ferlab.clin.qlinme.model.BatchStatus;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.MetadataHistory;
import bio.ferlab.clin.qlinme.services.MetadataValidationService;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BatchController {

  private final S3Client s3Client;
  private final String metadataBucket;
  private final MetadataValidationService metadataValidationService;


  @OpenApi(
    summary = "read metadata",
    description = "Return current metadata by batch_id",
    operationId = "batchRead",
    path = Routes.BATCH,
    methods = HttpMethod.GET,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", required = true),
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true),
    },
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = Metadata.class)),
      @OpenApiResponse(status = "403"),
      @OpenApiResponse(status = "404"),
    }
  )
  public void batchRead(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    try {
      ctx.contentType(ContentType.APPLICATION_JSON).result(s3Client.getMetadata(metadataBucket, batchId));
    } catch (Exception e) {
      ctx.status(HttpStatus.NOT_FOUND);
    }
  }

  @OpenApi(
    summary = "create or update metadata",
    description = "Create or update the metadata of a batch by batch_id and validate content",
    operationId = "batchCreateUpdate",
    path = Routes.BATCH,
    methods = HttpMethod.POST,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", required = true),
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true),
    },
    requestBody = @OpenApiRequestBody(
      description = "Metadata",
      content = @OpenApiContent(from = Metadata.class)
    ),
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = Metadata.class)),
      @OpenApiResponse(status = "400", content = @OpenApiContent(from = MetadataValidation.class)),
      @OpenApiResponse(status = "403"),
    }
  )
  public void batchCreateUpdate(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    var metadata = ctx.bodyAsClass(Metadata.class);
    var validation = metadataValidationService.validateMetadata(metadata, batchId);
    if (!validation.isValid()) {
      ctx.status(HttpStatus.BAD_REQUEST).json(validation);
    }else {
      s3Client.backupAndSaveMetadata(metadataBucket, batchId, ctx.body());
      ctx.json(metadata);
    }
  }

  @OpenApi(
    summary = "status",
    description = "Return status of the current batchs",
    operationId = "batchStatus",
    path = Routes.BATCH_STATUS,
    methods = HttpMethod.GET,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", required = true),
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true),
    },
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = BatchStatus.class)),
      @OpenApiResponse(status = "403"),
      @OpenApiResponse(status = "404"),
    }
  )
  public void batchStatus(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    // TODO coder la status
    ctx.json(new BatchStatus("not implemented"));
  }


  @OpenApi(
    summary = "history of versions",
    description = "Return previous versions of the metadata by batch id",
    operationId = "batchHistory",
    path = Routes.BATCH_HISTORY,
    methods = HttpMethod.GET,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", required = true),
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true),
    },
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = MetadataHistory.class)),
      @OpenApiResponse(status = "403"),
      @OpenApiResponse(status = "404"),
    }
  )
  public void batchHistory(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    var versions = s3Client.listBackupVersion(metadataBucket, batchId);
    ctx.json(versions.stream().map(o -> new MetadataHistory(extractVersion(o.key()), o.lastModified().toString())).toList());
  }

  private String extractVersion(String key) {
    var token = key.split("\\.");
    return token[token.length-1];
  }

  @OpenApi(
    summary = "read version",
    description = "Return a specific version of the metadata by batch id",
    operationId = "batchHistoryVersion",
    path = Routes.BATCH_HISTORY_BY_VERSION,
    methods = HttpMethod.GET,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", required = true),
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true),
    },
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = Metadata.class)),
      @OpenApiResponse(status = "400", content = @OpenApiContent(from = MetadataValidation.class)),
      @OpenApiResponse(status = "403"),
    }
  )
  public void batchHistoryByVersion(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    var version = Utils.getValidParamParam(ctx, "version").get();
    try {
      ctx.contentType(ContentType.APPLICATION_JSON).result(s3Client.getBackupMetadata(metadataBucket, batchId, version));
    } catch (Exception e) {
      ctx.status(HttpStatus.NOT_FOUND);
    }
  }

}
