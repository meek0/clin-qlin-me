package bio.ferlab.clin.qlinme.controllers;

import bio.ferlab.clin.qlinme.Routes;
import bio.ferlab.clin.qlinme.Utils;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.UserToken;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

public class BatchController {

  private final List<String> schemaValues = List.of("CQGC_Germline", "CQGC_Exome_Tumeur_Seul");
  private final List<String> ldmValues = List.of("LDM-CHUSJ", "LDM-CHUS", "LDM-CUSM");
  private final List<String> panelCodeValues = List.of("MMG", "DYSM", "RHAB", "MITN", "MYOC", "MYAC", "HYPM", "RGDI", "POLYM", "TRATU", "EXTUM", "EXNOR", "TUPED", "TUHEM");
  private final List<String> sampleTypeValues = List.of("DNA");
  private final List<String> specimenTypeValues = List.of("NBL", "TUMOR");
  private final List<String> designFamilyValues = List.of("SOLO", "DUO", "TRIO");
  private final List<String> epValues = List.of("CHUSJ", "CHUS", "CUSM");
  private final List<String> familyMemberValues = List.of("PROBAND", "MTH", "FTH", "SIS", "BRO");
  private final List<String> fetusValues = List.of("false");
  private final List<String> sexValues = List.of("female", "male", "unknown");
  private final List<String> statusValues = List.of("AFF", "UNF", "UNK");
  private final List<String> versionValues = List.of("3.8.4", "4.2.4");
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
      .check("ldm", m -> validateAnalysesField(m, "ldm", ldmValues), "should be " + ldmValues)
      .check("panelCode",  m -> validateAnalysesField(m, "panelCode", panelCodeValues), "should be " + panelCodeValues)
      .check("sampleType", m -> validateAnalysesField(m, "sampleType", sampleTypeValues), "should be " + sampleTypeValues)
      .check("specimenType", m -> validateAnalysesField(m, "specimenType", specimenTypeValues), "should be " + specimenTypeValues)
      .check("designFamily",  m -> validateAnalysesField(m, "designFamily", designFamilyValues), "should be " + designFamilyValues)
      .check("ep",  m -> validateAnalysesField(m, "ep", epValues), "should be " + epValues)
      .check("familyMember",m -> validateAnalysesField(m, "familyMember", familyMemberValues), "should be " + familyMemberValues)
      .check("fetus", m -> validateAnalysesField(m, "fetus", fetusValues), "should be " + fetusValues)
      .check("sex", m -> validateAnalysesField(m, "sex", sexValues), "should be " + sexValues)
      .check("status",  m -> validateAnalysesField(m, "status", statusValues), "should be " + statusValues)
      .check("version", m -> validateAnalysesField(m, "version", versionValues), "should be " + versionValues);
    ctx.json(validations.get());
  }


  private boolean validate(Metadata m, String field) {
    return m != null && StringUtils.isNotBlank(field);
  }

  private boolean validateSubmissionSchema(Metadata m) {
    return validate(m, m.submissionSchema()) && schemaValues.contains(m.submissionSchema());
  }

  private boolean validateAnalysesField(Metadata m, String fieldName, List<?> values) {
    boolean isValid = m != null && m.analyses() != null && !m.analyses().isEmpty();
    if (isValid) {
      for (var ana : m.analyses()) {
        String fieldValue = null;
        switch (fieldName){
          case "ldm":
            fieldValue = ana.ldm();
            break;
          case "panelCode":
            fieldValue = Optional.ofNullable(ana.analysisCode()).filter(StringUtils::isNotBlank).orElse(ana.panelCode());
            break;
          case "sampleType":
            fieldValue = ana.sampleType();
            break;
          case "specimenType":
            fieldValue = ana.specimenType();
            break;
          case "designFamily":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::designFamily).orElse(null);
            break;
          case "ep":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::ep).orElse(null);
            break;
          case "familyMember":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::familyMember).orElse(null);
            break;
          case "fetus":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::fetus).map(String::valueOf).orElse(null);
            break;
          case "sex":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::sex).orElse(null);
            break;
          case "status":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::status).orElse(null);
            break;
          case "version":
            fieldValue = Optional.ofNullable(ana.workflow()).map(Metadata.Workflow::version).orElse(null);
            break;
        }
        isValid = isValid & validate(m, fieldValue) && values.contains(fieldValue);
      }
    }
    return isValid;
  }
}
