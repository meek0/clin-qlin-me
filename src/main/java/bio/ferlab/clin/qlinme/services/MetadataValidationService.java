package bio.ferlab.clin.qlinme.services;

import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class MetadataValidationService {

  public enum SchemaValues {
      CQGC_Germline, CQGC_Exome_Tumeur_Seul
  }

  public enum Files {
    cram,
    crai,
    snv_vcf,
    snv_tbi,
    cnv_vcf,
    cnv_tbi,
    sv_vcf,
    sv_tbi,
    supplement,
    exomiser_html,
    exomiser_json,
    exomiser_variants_tsv,
    seg_bw,
    hard_filtered_baf_bw,
    roh_bed,
    hyper_exome_hg38_bed,
    cnv_calls_png,
    coverage_by_gene_csv,
    qc_metrics
  }

  private final List<String> schemaValues =  Arrays.stream(SchemaValues.values()).map(Enum::name).toList();
  private final List<String> ldmValues = List.of("LDM-CHUSJ", "LDM-CHUS", "LDM-CUSM");
  private final List<String> panelCodeValues = List.of("MMG", "DYSM", "RHAB", "MITN", "MYOC", "MYAC", "HYPM", "RGDI", "POLYM", "TRATU", "EXTUM", "EXNOR", "TUPED", "TUHEM");
  private final List<String> sampleTypeValues = List.of("DNA");
  private final List<String> specimenTypeValues = List.of("NBL", "TUMOR");
  private final List<String> designFamilyValues = List.of("SOLO", "DUO", "TRIO");
  private final List<String> epValues = List.of("CHUSJ", "CHUS", "CUSM");
  private final List<String> familyMemberValues = List.of("PROBAND", "MTH", "FTH", "SIS", "BRO");
  private final List<String> fetusValues = List.of("false", "null");
  private final List<String> sexValues = List.of("female", "male", "unknown");
  private final List<String> statusValues = List.of("AFF", "UNF", "UNK");
  private final List<String> versionValues = List.of("3.8.4","3.10.4", "4.2.4");
  private final List<String> fileKeys =  Arrays.stream(Files.values()).map(Enum::name).toList();

  public MetadataValidation validateMetadata(Metadata m, String batchId) {
    var validation = new MetadataValidation();
    Map<String, List<String>> valuesByField = new TreeMap<>();
    if (m != null) {
      validateField("submissionSchema", m.submissionSchema(), validation, schemaValues);
      validation.setSchema(m.submissionSchema());
      if (!m.analyses().isEmpty()) {
        validation.setAnalysesCount(m.analyses().size());
        for (int ai = 0; ai < m.analyses().size(); ai++) {
          var ana = m.analyses().get(ai);
          var errorPrefix = "analyses[" + ai + "]";
          String familyMember = null;

          validateField(errorPrefix + ".ldm", ana.ldm(), validation, ldmValues);
          validateField(errorPrefix + ".ldmSpecimenId", ana.ldmSpecimenId(), validation, null);
          validateField(errorPrefix + ".ldmSampleId", ana.ldmSampleId(), validation, null);
          validateField(errorPrefix + ".labAliquotId", ana.labAliquotId(), validation, null);
          checkUnicity("labAliquotId", errorPrefix + ".labAliquotId", ana.labAliquotId(), valuesByField, validation);

          var panelCode = Optional.ofNullable(ana.analysisCode()).filter(StringUtils::isNotBlank).orElse(ana.panelCode());
          validateField(errorPrefix + ".panelCode", panelCode, validation, panelCodeValues);

          validateField(errorPrefix + ".sampleType", ana.sampleType(), validation, sampleTypeValues);
          validateField(errorPrefix + ".ldmServiceRequestId", ana.ldmServiceRequestId(), validation, null);
          validateField(errorPrefix + ".specimenType", ana.specimenType(), validation, specimenTypeValues);
          if (ana.patient() != null) {
            var patient = ana.patient();
            familyMember = patient.familyMember();
            validateField(errorPrefix + ".patient.firstName", patient.firstName(), validation, null);
            validateField(errorPrefix + ".patient.lastName", patient.lastName(), validation, null);
            validateField(errorPrefix + ".patient.designFamily", patient.designFamily(), validation, designFamilyValues);
            validateField(errorPrefix + ".patient.ep", patient.ep(), validation, epValues);
            validateField(errorPrefix + ".patient.familyMember", familyMember, validation, familyMemberValues);
            validateField(errorPrefix + ".patient.fetus", String.valueOf(patient.fetus()), validation, fetusValues);
            validateField(errorPrefix + ".patient.sex", StringUtils.toRootLowerCase(patient.sex()), validation, sexValues);
            validateField(errorPrefix + ".patient.birthDate", patient.birthDate(), validation, null);
            validateField(errorPrefix + ".patient.status", patient.status(), validation, statusValues);
            validatePatient(errorPrefix + ".patient", patient, validation);
            validateFamilyId(errorPrefix + ".patient", patient, validation);
            checkUnicity("mrn", errorPrefix + ".patient.mrn", patient.mrn(), valuesByField, validation);
            checkUnicity("ramq", errorPrefix + ".patient.ramq", patient.ramq(), valuesByField, validation);
          } else {
            validation.addError(errorPrefix + ".patient", "is required");
          }

          if (ana.experiment() != null) {
            var exp = ana.experiment();
            validateField(errorPrefix + ".experiment.platform", exp.platform(), validation, null);
            validateField(errorPrefix + ".experiment.sequencerId", exp.sequencerId(), validation, null);
            validateField(errorPrefix + ".experiment.runName", exp.runName(), validation, null);
            validateRunName(errorPrefix + ".experiment.runName", exp.runName(), validation, batchId);
            validateField(errorPrefix + ".experiment.runDate", exp.runDate(), validation, null);
            validateField(errorPrefix + ".experiment.runAlias", exp.runAlias(), validation, null);
            validateField(errorPrefix + ".experiment.flowcellId", exp.flowcellId(), validation, null);
            validateField(errorPrefix + ".experiment.isPairedEnd", (exp.isPairedEnd() != null) ? String.valueOf(exp.isPairedEnd()) : null, validation, null);
            validateField(errorPrefix + ".experiment.fragmentSize", (exp.fragmentSize() != null) ? String.valueOf(exp.fragmentSize()) : null, validation, null);
            validateField(errorPrefix + ".experiment.experimentalStrategy", exp.experimentalStrategy(), validation, null);
            validateField(errorPrefix + ".experiment.captureKit", exp.captureKit(), validation, null);
            validateField(errorPrefix + ".experiment.baitDefinition", exp.baitDefinition(), validation, null);
          } else {
            validation.addError(errorPrefix + ".experiment", "is required");
          }

          if (ana.workflow() != null) {
            var work = ana.workflow();
            validateField(errorPrefix + ".workflow.name", work.name(), validation, null);
            validateField(errorPrefix + ".workflow.version", work.version(), validation, versionValues);
            validateField(errorPrefix + ".workflow.genomeBuild", work.genomeBuild(), validation, null);
          } else {
            validation.addError(errorPrefix + ".workflow", "is required");
          }

          if (ana.files() != null) {
            var files = ana.files();
            for(String fileKey: files.keySet()) {

              if(!fileKeys.contains(fileKey)) {
                validation.addError(errorPrefix + ".files."+fileKey, "Supported files are: "+ fileKeys);
              }
            }
            if ("MTH".equals(familyMember) || "FTH".equals(familyMember)) {
              validateExomiser(errorPrefix + ".files", files, validation);
            }
          } else {
            validation.addError(errorPrefix + ".files", "is required");
          }
        }
      } else {
        validation.addError("analyses", "is required");
      }
    } else {
      validation.addError("metadata", "is required");
    }
    return validation;
  }

  private void validateField(String field, String value, MetadataValidation validation, List<String> values) {
    if (StringUtils.isBlank(value)) {
      if (values != null && !values.isEmpty()) {
        validation.addError(field, "should be " + values);
      } else {
        validation.addError(field, "is missing");
      }
    } else if (values != null && !values.contains(value)) {
      validation.addError(field, value + " should be " + values);
    }
  }

  private void validateRunName(String field, String runName, MetadataValidation validation, String batchId) {
    if (runName != null && !batchId.contains(runName)) {
      validation.addError(field, runName + " should be similar to batch_id: " + batchId);
    }
  }

  private void validatePatient(String field, Metadata.Patient patient, MetadataValidation validation) {
    if (StringUtils.isAllBlank(patient.mrn(), patient.ramq())) {
      validation.addError(field, "should have mrn or ramq or both");
    }
  }

  private void validateFamilyId(String field, Metadata.Patient patient, MetadataValidation validation) {
    if ("SOLO".equals(patient.designFamily()) && StringUtils.isNotBlank(patient.familyId())) {
      validation.addError(field, "designFamily SOLO should not have familyId");
    }
  }

  private void validateExomiser(String field, Map<String, String> files, MetadataValidation validation) {
    if (!StringUtils.isAllBlank(files.get(Files.exomiser_html.name()), files.get(Files.exomiser_json.name()), files.get(Files.exomiser_variants_tsv.name()))) {
      validation.addError(field, "familyMember other than PROBAND|SIS|BRO should not have exomiser files");
    }
  }

  private void checkUnicity(String globalField, String field, String value, Map<String, List<String>> valuesByField, MetadataValidation validation) {
    valuesByField.computeIfAbsent(globalField, f -> new ArrayList<>());
    var previousFieldValues = valuesByField.get(globalField);
    if (value != null) {
      if (previousFieldValues.contains(value)) {
        validation.addError(field, "should be unique");
      } else {
        previousFieldValues.add(value);
      }
    }
  }


}
