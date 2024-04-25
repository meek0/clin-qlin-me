package bio.ferlab.clin.qlinme.controllers;

import bio.ferlab.clin.qlinme.Routes;
import bio.ferlab.clin.qlinme.Utils;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.UserToken;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class BatchController {

  private final List<String> schemaValues = List.of("CQGC_Germline", "CQGC_Exome_Tumeur_Seul");

  @OpenApi(
    summary = "Create or update batch",
    description = "Crate or update batch by batch_id and validate content",
    operationId = "batchCreateUpdate",
    path = Routes.BATCH_POST,
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
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserToken.class)),
      @OpenApiResponse(status = "400"),
      @OpenApiResponse(status = "403"),
    }
  )
  public void batchCreateUpdate(Context ctx) {
    var batchId = Utils.getValidParamParam(ctx, "batch_id").get();
    var validations = ctx.bodyValidator(Metadata.class)
      .check("submissionSchema", this::validateSubmissionSchema, "should be " + schemaValues)
      .check("analyses", this::validateAnalyses, "can't be empty")
      .check("ldm", this::validateAnalysesLdm, "is required");
    ctx.json(validations.get());
  }

  private boolean validate(Metadata m, String field) {
    return m != null && StringUtils.isNotBlank(field);
  }

  private boolean validate(Metadata m, List<?> list) {
    return m != null && list != null && !list.isEmpty();
  }

  private boolean validateSubmissionSchema(Metadata m) {
    return validate(m, m.submissionSchema()) && schemaValues.contains(m.submissionSchema());
  }

  private boolean validateAnalyses(Metadata m) {
    return validate(m, m.analyses());
  }

  private boolean validateAnalysesLdm(Metadata m) {
    boolean isValid = validate(m, m.analyses());
    if (isValid) {
      for (var ana : m.analyses()) {
        isValid = isValid & validate(m, ana.ldm());
      }
    }
    return isValid;
  }
}
