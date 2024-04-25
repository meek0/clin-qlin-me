package bio.ferlab.clin.qlinme.controllers;

import bio.ferlab.clin.qlinme.Utils;
import bio.ferlab.clin.qlinme.model.Metadata;
import io.javalin.http.Context;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class BatchController {

  private final List<String> schemaValues = List.of("CQGC_Germline", "CQGC_Exome_Tumeur_Seul");

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
