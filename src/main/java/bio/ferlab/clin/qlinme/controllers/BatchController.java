package bio.ferlab.clin.qlinme.controllers;

import bio.ferlab.clin.qlinme.Routes;
import bio.ferlab.clin.qlinme.Utils;
import bio.ferlab.clin.qlinme.cients.S3Client;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.UserToken;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@RequiredArgsConstructor
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

  private final S3Client s3Client;
  private final String metadataBucket;

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
      .check("runName", m -> validateRunName(m, batchId), "should be similar to batch_id ("+batchId+")")
      .check("submissionSchema", this::validateSubmissionSchema, "should be " + schemaValues)
      .check("ldm", m -> validateAnalysesField(m, "ldm", ldmValues, false, false), "should be " + ldmValues)
      .check("panelCode",  m -> validateAnalysesField(m, "panelCode", panelCodeValues, false, false), "should be " + panelCodeValues)
      .check("sampleType", m -> validateAnalysesField(m, "sampleType", sampleTypeValues, false,false), "should be " + sampleTypeValues)
      .check("specimenType", m -> validateAnalysesField(m, "specimenType", specimenTypeValues, false, false), "should be " + specimenTypeValues)
      .check("designFamily",  m -> validateAnalysesField(m, "designFamily", designFamilyValues, false, false), "should be " + designFamilyValues)
      .check("ep",  m -> validateAnalysesField(m, "ep", epValues, false, false), "should be " + epValues)
      .check("familyMember",m -> validateAnalysesField(m, "familyMember", familyMemberValues, false, false), "should be " + familyMemberValues)
      .check("fetus", m -> validateAnalysesField(m, "fetus", fetusValues, false, false), "should be " + fetusValues)
      .check("sex", m -> validateAnalysesField(m, "sex", sexValues, false, false), "should be " + sexValues)
      .check("status",  m -> validateAnalysesField(m, "status", statusValues, false, false), "should be " + statusValues)
      .check("patient",  m -> validateAnalysesField(m, "patient", null, false, false), "should have mrn or ramq or both")
      .check("familyId",  m -> validateAnalysesField(m, "familyId", null, false, false), "designFamily SOLO should not have familyId")
      .check("version", m -> validateAnalysesField(m, "version", versionValues, false, false), "should be " + versionValues)
      .check("labAliquotId", m -> validateAnalysesField(m, "labAliquotId", null, true, false), "should be unique")
      .check("mrn", m -> validateAnalysesField(m, "mrn", null, true, true), "should be unique")
      .check("ramq", m -> validateAnalysesField(m, "ramq", null, true, true), "should be unique")
      .check("exomiser", m -> validateAnalysesField(m, "exomiser", null, false, false), "familyMember other than PROBAND should not have exomiser files");
    var metadata = validations.get();
    s3Client.saveMetadata(metadataBucket, batchId, ctx.body());
    ctx.json(metadata);
  }

  private boolean validateRunName(Metadata m, String batchId) {
    boolean isValid = m != null && m.analyses() != null && !m.analyses().isEmpty();
    if (isValid) {
      for (var ana : m.analyses()) {
        isValid = isValid & Optional.ofNullable(ana.experiment()).map(Metadata.Experiment::runName).filter(batchId::contains).isPresent();
      }
    }
    return isValid;
  }

  private boolean validate(Metadata m, String field) {
    return m != null && StringUtils.isNotBlank(field);
  }

  private boolean validateSubmissionSchema(Metadata m) {
    return validate(m, m.submissionSchema()) && schemaValues.contains(m.submissionSchema());
  }

  private boolean validateAnalysesField(Metadata m, String fieldName, List<?> values, boolean unique, boolean allowNullable) {
    boolean isValid = m != null && m.analyses() != null && !m.analyses().isEmpty();
    Map<String, List<String>> valuesByField = new TreeMap<>();
    if (isValid) {
      for (var ana : m.analyses()) {
        String fieldValue = null;
        var familyMember = Optional.ofNullable(ana.patient()).map(Metadata.Patient::familyMember).orElse(null);
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
            fieldValue = familyMember;
            break;
          case "fetus":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::fetus).map(String::valueOf).orElse(null);
            break;
          case "sex":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::sex).map(String::toLowerCase).orElse(null);
            break;
          case "status":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::status).orElse(null);
            break;
          case "version":
            fieldValue = Optional.ofNullable(ana.workflow()).map(Metadata.Workflow::version).orElse(null);
            break;
          case "patient":
            var mrn = Optional.ofNullable(ana.patient()).map(Metadata.Patient::mrn);
            var ramq = Optional.ofNullable(ana.patient()).map(Metadata.Patient::ramq);
            fieldValue = mrn.or(() -> ramq).orElse(null);
            break;
          case "familyId":
            var designFamily = Optional.ofNullable(ana.patient()).map(Metadata.Patient::designFamily).orElse(null);
            if ("SOLO".equals(designFamily)) {
              fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::familyId).isPresent() ? null : "OK";
            } else {
              fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::familyId).orElse(null);
            }
            break;
          case "labAliquotId":
            fieldValue = Optional.ofNullable(ana).map(Metadata.Analysis::labAliquotId).orElse(null);
            break;
          case "mrn":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::mrn).orElse(null);
            break;
          case "ramq":
            fieldValue = Optional.ofNullable(ana.patient()).map(Metadata.Patient::ramq).orElse(null);
            break;
          case "exomiser":
            var e1 = Optional.ofNullable(ana.files()).map(Metadata.Files::exomiser_html).orElse(null);
            var e2 = Optional.ofNullable(ana.files()).map(Metadata.Files::exomiser_json).orElse(null);
            var e3 = Optional.ofNullable(ana.files()).map(Metadata.Files::exomiser_variants_tsv).orElse(null);
            if (!"PROBAND".equals(familyMember)) {
              fieldValue = StringUtils.isAllBlank(e1, e2, e3) ? "OK" : null;
            } else {
              fieldValue = "OK";
            }
            break;
        }
        isValid = isValid & (allowNullable || (validate(m, fieldValue) && (values == null || values.contains(fieldValue))));
        // unicity
        valuesByField.computeIfAbsent(fieldName, f -> new ArrayList<>());
        var previousFieldValues = valuesByField.get(fieldName);
        if (unique && fieldValue != null) {
          if (previousFieldValues.contains(fieldValue)) {
            isValid = false;
          } else {
            previousFieldValues.add(fieldValue);
          }
        }
      }
    }
    return isValid;
  }
}
