package bio.ferlab.clin.qlinme.controllers;

import bio.ferlab.clin.qlinme.Routes;
import bio.ferlab.clin.qlinme.cients.FhirClient;
import bio.ferlab.clin.qlinme.cients.S3Client;
import bio.ferlab.clin.qlinme.model.BatchStatus;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.MetadataHistory;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import bio.ferlab.clin.qlinme.services.FilesValidationService;
import bio.ferlab.clin.qlinme.services.MetadataValidationService;
import bio.ferlab.clin.qlinme.services.VCFsValidationService;
import bio.ferlab.clin.qlinme.utils.Utils;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import io.javalin.openapi.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BatchController {

  private final S3Client s3Client;
  private final String metadataBucket;
  private final MetadataValidationService metadataValidationService;
  private final FilesValidationService filesValidationService;
  private final VCFsValidationService vcFsValidationService;
  private final JavalinJackson objectMapper;
  private final FhirClient fhirClient;

  @OpenApi(
    summary = "Get current metadata",
    description = "Get current metadata by batch_id",
    operationId = "batchRead",
    path = Routes.BATCH,
    methods = HttpMethod.GET,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", example = "Bearer aaa.bbb.ccc", required = true),
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true, description = "Should match the batch folder name in S3"),
    },
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = Metadata.class)),
      @OpenApiResponse(status = "403", description = "Bearer token is missing or invalid"),
      @OpenApiResponse(status = "404", description = "Batch doesn't exist"),
    }
  )
  public void batchRead(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    try {
      ctx.contentType(ContentType.APPLICATION_JSON).result(s3Client.getMetadata(metadataBucket, batchId));
    } catch (NoSuchKeyException e) {
      ctx.status(HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @OpenApi(
    summary = "Upload metadata",
    description = "Create or update the metadata by batch_id and validate content",
    operationId = "batchCreateUpdate",
    path = Routes.BATCH,
    methods = HttpMethod.POST,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", example = "Bearer aaa.bbb.ccc", required = true),
      @OpenApiParam(name = HttpHeaders.CACHE_CONTROL , description = "Ignore and refresh previous cached data used for validation", example = "no-cache")
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true, description = "Should match the batch folder name in S3"),
    },
    queryParams = {
      @OpenApiParam(name = "validate-only", type = Boolean.class, allowEmptyValue = true, description = "Optional, validate only and ignore saving the metadata", example = "<empty>|true"),
    },
    requestBody = @OpenApiRequestBody(
      description = "Metadata JSON",
      content = @OpenApiContent(from = Metadata.class)
    ),
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = Metadata.class), description = "Metadata is valid but not saved"),
      @OpenApiResponse(status = "201", content = @OpenApiContent(from = Metadata.class), description = "Metadata is valid and saved"),
      @OpenApiResponse(status = "400", content = @OpenApiContent(from = MetadataValidation.class), description = "Metadata is invalid and contains errors to be fixed"),
      @OpenApiResponse(status = "403", description = "Bearer token is missing or invalid"),
    }
  )
  public void batchCreateUpdate(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    var allowCache = !"no-cache".equals(ctx.header(HttpHeaders.CACHE_CONTROL));
    var validate = List.of("", "true").contains(String.valueOf(ctx.queryParam("validate-only"))); // "null" wont work
    var metadata = ctx.bodyAsClass(Metadata.class);
    validateAndCreateMetadata(ctx, metadata, batchId, !validate, allowCache);
  }

  private MetadataValidation validate(Context ctx, Metadata metadata, String batchId, boolean allowCache) {
    var rpt = ctx.header(HttpHeaders.AUTHORIZATION);
    return metadataValidationService.validateMetadata(metadata, batchId,
      fhirClient.getPanelCodes(rpt, allowCache),
      fhirClient.getOrganizations(rpt, allowCache),
      fhirClient.getAliquotIDsByBatch(rpt, metadataValidationService.extractAliquotIDs(metadata), allowCache),
      fhirClient.getPatients(rpt, metadataValidationService.extractMRNs(metadata), metadataValidationService.extractRAMQs(metadata), allowCache));
  }

  private void validateAndCreateMetadata(Context ctx, Metadata metadata, String batchId, boolean save, boolean allowCache) {
    var validation = validate(ctx, metadata, batchId, allowCache);
    if (!validation.isValid()) {
      ctx.status(HttpStatus.BAD_REQUEST).json(validation);
    }else {
      try {
        if (save) {
          var validatedMetadataStr = objectMapper.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
          s3Client.backupAndSaveMetadata(metadataBucket, batchId, validatedMetadataStr);
          ctx.status(HttpStatus.CREATED).json(metadata);
        } else {
          ctx.json(metadata);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @OpenApi(
    summary = "Get status",
    description = "Return status of the batch by id",
    operationId = "batchStatus",
    path = Routes.BATCH_STATUS,
    methods = HttpMethod.GET,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", example = "Bearer aaa.bbb.ccc", required = true),
      @OpenApiParam(name = HttpHeaders.CACHE_CONTROL , description = "Ignore and refresh previous cached data used for validation", example = "no-cache")
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true, description = "Should match the batch folder name in S3"),
    },
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = BatchStatus.class)),
      @OpenApiResponse(status = "403", description = "Bearer token is missing or invalid"),
      @OpenApiResponse(status = "404", description = "Batch doesn't exist"),
    }
  )
  public void batchStatus(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    var allowCache = !"no-cache".equals(ctx.header(HttpHeaders.CACHE_CONTROL));
    try {
      var metadata = objectMapper.getMapper().readValue(s3Client.getMetadata(metadataBucket, batchId), Metadata.class);
      var metadataValidation = validate(ctx, metadata, batchId, allowCache);
      var s3Files = s3Client.listBatchFiles(metadataBucket, batchId);
      var filesValidation = filesValidationService.validateFiles(metadata, s3Files);
      var vcfsValidation = vcFsValidationService.validate(metadata, batchId, s3Files, allowCache);
      var status = (metadataValidation.isValid() & filesValidation.isValid() & vcfsValidation.isValid()) ? "READY_TO_IMPORT" : "ERRORS";
      ctx.json(new BatchStatus(status, metadataValidation, filesValidation, vcfsValidation));
    } catch (NoSuchKeyException e) {
      ctx.status(HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @OpenApi(
    summary = "Get previous versions",
    description = "Return previous versions of the metadata by batch id",
    operationId = "batchHistory",
    path = Routes.BATCH_HISTORY,
    methods = HttpMethod.GET,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", example = "Bearer aaa.bbb.ccc" , required = true),
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true, description = "Should match the batch folder name in S3"),
    },
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = MetadataHistory.class)),
      @OpenApiResponse(status = "403", description = "Bearer token is missing or invalid"),
      @OpenApiResponse(status = "404", description = "Batch doesn't exist"),
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
    summary = "Get previous version",
    description = "Return a specific version of the metadata by batch id",
    operationId = "batchHistoryVersion",
    path = Routes.BATCH_HISTORY_BY_VERSION,
    methods = HttpMethod.GET,
    tags = {"Batch"},
    headers = {
      @OpenApiParam(name = "Authorization", example = "Bearer aaa.bbb.ccc" , required = true),
    },
    pathParams = {
      @OpenApiParam(name = "batch_id", required = true, description = "Should match the batch folder name in S3"),
    },
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = Metadata.class)),
      @OpenApiResponse(status = "403", description = "Bearer token is missing or invalid"),
      @OpenApiResponse(status = "404", description = "Batch doesn't exist"),
    }
  )
  public void batchHistoryByVersion(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    var version = Utils.getValidParamParam(ctx, "version").get();
    try {
      ctx.contentType(ContentType.APPLICATION_JSON).result(s3Client.getBackupMetadata(metadataBucket, batchId, version));
    } catch (NoSuchKeyException e) {
      ctx.status(HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
