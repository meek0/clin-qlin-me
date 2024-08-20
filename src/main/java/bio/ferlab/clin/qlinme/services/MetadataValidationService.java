package bio.ferlab.clin.qlinme.services;

import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import bio.ferlab.clin.qlinme.utils.DateUtils;
import bio.ferlab.clin.qlinme.utils.NameUtils;
import bio.ferlab.clin.qlinme.utils.Utils;
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

  enum SpecimenType {
    NBL, TUMOR
  }

  private final List<String> schemaValues =  Arrays.stream(SchemaValues.values()).map(Enum::name).toList();
  private final List<String> sampleTypeValues = List.of("DNA");
  private final List<String> specimenTypeValues = Arrays.stream(SpecimenType.values()).map(Enum::name).toList();
  private final List<String> familyMemberValues = List.of("PROBAND", "MTH", "FTH", "SIS", "BRO");
  private final List<String> fetusValues = List.of("false", "null");
  private final List<String> sexValues = List.of("female", "male", "unknown");
  private final List<String> statusValues = List.of("AFF", "UNF", "UNK");
  private final List<String> versionValues = List.of("3.8.4","3.10.4", "4.2.4");
  private final List<String> fileKeys =  Arrays.stream(Files.values()).map(Enum::name).toList();

  record Family(List<String> members){}

  public MetadataValidation validateMetadata(Metadata m, String batchId, List<String> panelCodeValues, List<String> organizations, Map<String, List<String>> aliquotIDsByBatch, Map<String, List<String>> ldmServiceRequestsByBatch, List<Metadata.Patient> patients) {
    var validation = new MetadataValidation();
    Map<String, List<String>> valuesByField = new TreeMap<>();
    Map<String, Family>families = new TreeMap<>();
    Set<String> validRunNames = new TreeSet<>();
    var ldmValues = organizations.stream().filter(o -> o.startsWith("LDM")).toList();
    var epValues = organizations.stream().filter(o -> !o.startsWith("LDM")).toList();
    if (m != null) {
      validateField("submissionSchema", m.submissionSchema(), validation, schemaValues);
      validation.setSchema(m.submissionSchema());
      validation.setBatchId(batchId);
      if (m.analyses() != null && !m.analyses().isEmpty()) {
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
          checkAliquotId(errorPrefix + ".labAliquotId", ana.labAliquotId(), validation, batchId, aliquotIDsByBatch);
          checkLdmServiceRequestId(errorPrefix + ".ldmServiceRequestId", ana.ldmServiceRequestId(), validation, batchId, ldmServiceRequestsByBatch);

          var panelCode = Optional.ofNullable(ana.analysisCode()).orElse(ana.panelCode());
          var panelCodeField = Optional.ofNullable(ana.analysisCode()).map(e -> "analysisCode").orElse("panelCode");
          validateField(errorPrefix + "."+panelCodeField, panelCode, validation, panelCodeValues);
          validatePanelCode(errorPrefix + "."+panelCodeField, m, panelCode, validation, panelCodeValues);

          validateField(errorPrefix + ".sampleType", ana.sampleType(), validation, sampleTypeValues);
          validateField(errorPrefix + ".ldmServiceRequestId", ana.ldmServiceRequestId(), validation, null);
          validateField(errorPrefix + ".specimenType", ana.specimenType(), validation, specimenTypeValues);
          validateSpecimenType(errorPrefix + ".specimenType", m, ana.specimenType(), validation);
          if (ana.patient() != null) {
            var patient = ana.patient();
            familyMember = patient.familyMember();
            validateField(errorPrefix + ".patient.firstName", patient.firstName(), validation, null);
            validateSpecialCharacters(errorPrefix+ ".patient.firstName", patient.firstName(), validation);
            validateField(errorPrefix + ".patient.lastName", patient.lastName(), validation, null);
            validateSpecialCharacters(errorPrefix+ ".patient.lastName", patient.lastName(), validation);
            validateField(errorPrefix + ".patient.ep", patient.ep(), validation, epValues);
            validateField(errorPrefix + ".patient.familyMember", familyMember, validation, familyMemberValues);
            validateField(errorPrefix + ".patient.fetus", String.valueOf(patient.fetus()), validation, fetusValues);
            validateField(errorPrefix + ".patient.sex", StringUtils.toRootLowerCase(patient.sex()), validation, sexValues);
            validateField(errorPrefix + ".patient.birthDate", patient.birthDate(), validation, null);
            validateDate(errorPrefix+ ".patient.birthDate", patient.birthDate(), validation, DateUtils.DDMMYYYY);
            validateField(errorPrefix + ".patient.status", patient.status(), validation, statusValues);
            validatePatient(errorPrefix + ".patient", patient, validation);
            validatePatientDetails(errorPrefix + ".patient", patient, validation, patients);
            checkUnicity("mrn", errorPrefix + ".patient.mrn", patient.mrn(), valuesByField, validation);
            validateSpecialCharacters(errorPrefix+ ".patient.mrn", patient.mrn(), validation);
            checkUnicity("ramq", errorPrefix + ".patient.ramq", patient.ramq(), valuesByField, validation);
            validateRamq(errorPrefix + ".patient.ramq", patient.ramq(), validation);
            updateFamilies(patient, families);
          } else {
            validation.addError(errorPrefix + ".patient", "is required");
          }

          if (ana.experiment() != null) {
            var exp = ana.experiment();
            validateField(errorPrefix + ".experiment.platform", exp.platform(), validation, null);
            validateField(errorPrefix + ".experiment.sequencerId", exp.sequencerId(), validation, null);
            validateField(errorPrefix + ".experiment.runName", exp.runName(), validation, null);
            validateRunName(errorPrefix + ".experiment.runName", exp.runName(), validation, batchId, validRunNames);
            validateField(errorPrefix + ".experiment.runDate", exp.runDate(), validation, null);
            validateDate(errorPrefix+ ".experiment.runDate", exp.runDate(), validation, DateUtils.DDMMYYYY, DateUtils.YYYYMMDD);
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
    validateFamilies(families, validation);
    if (validRunNames.isEmpty()) {
      validation.addError("Experiment.runName", "should have at least one valid runName");
    }
    return validation;
  }

  private void validatePatientDetails(String field, Metadata.Patient patient, MetadataValidation validation, List<Metadata.Patient> patients) {
    var existing = patients.stream()
      .filter(p -> (p.mrn() != null && p.mrn().equals(patient.mrn()) && (p.ep() != null && p.ep().equals(patient.ep())))
        || (p.ramq() != null && p.ramq().equals(patient.ramq()))).findFirst();
    existing.ifPresent((e) -> {
      validatePatientDetail(field+".mrn", patient.mrn(), e.mrn(), validation, false);
      validatePatientDetail(field+".ramq", patient.ramq(), e.ramq(), validation, false);
      validatePatientDetail(field+".firstName", patient.firstName(), e.firstName(), validation, true);
      validatePatientDetail(field+".lastName", patient.lastName(), e.lastName(), validation, true);
      validatePatientDetail(field+".sex", StringUtils.toRootLowerCase(patient.sex()), StringUtils.toRootLowerCase(e.sex()), validation, true);
      validatePatientDetail(field+".birthDate", patient.birthDate(), e.birthDate(), validation, true);
    });
  }

  private void validatePatientDetail(String field, String value, String existing, MetadataValidation validation, boolean warningOnly) {
    if (StringUtils.isNotBlank(existing) && !existing.equals(value)) {
      if (warningOnly) {
        validation.addWarning(field, "'" + value + "'" + " should be: " + existing);
      } else {
        validation.addError(field, "'" + value + "'" + " should be: " + existing);
      }
    }
  }

  private void validatePanelCode(String field, Metadata m, String value, MetadataValidation validation, List<String> panelCodeValues) {
    if (SchemaValues.CQGC_Exome_Tumeur_Seul.name().equals(m.submissionSchema())) {
      if (!"EXTUM".equals(value)) {
        validation.addError(field, "should be EXTUM for schema: " + SchemaValues.CQGC_Exome_Tumeur_Seul);
      }
    }

    if(SchemaValues.CQGC_Germline.name().equals(m.submissionSchema())) {
      if ("EXTUM".equals(value)) {
        validation.addError(field, "shouldn't be EXTUM for schema: " + SchemaValues.CQGC_Germline);
      }
    }
  }

  private void checkAliquotId(String field, String value, MetadataValidation validation, String currentBatchId, Map<String, List<String>> aliquotIDsByBatch) {
    if (StringUtils.isNotBlank(value)) {
      for (var batchId: aliquotIDsByBatch.keySet()) {
        var existing = aliquotIDsByBatch.get(batchId).stream().filter(value::equals).findFirst();
        if (existing.isPresent() && !currentBatchId.equals(batchId)) {
          validation.addError(field, "'" + value + "'" + " should be unique and exists in another batch: " + batchId);
          break;
        }
      }
    }
  }

  private void checkLdmServiceRequestId(String field, String value, MetadataValidation validation, String currentBatchId, Map<String, List<String>> ldmServiceRequestsByBatch) {
    if (StringUtils.isNotBlank(value)) {
      for (var batchId: ldmServiceRequestsByBatch.keySet()) {
        var existing = ldmServiceRequestsByBatch.get(batchId).stream().filter(value::equals).findFirst();
        if (existing.isPresent() && !currentBatchId.equals(batchId)) {
          validation.addError(field, "'" + value + "'" + " should be unique and exists in another batch: " + batchId);
          break;
        }
      }
    }
  }

  private void validateFamilies(Map<String, Family> families, MetadataValidation validation) {
    for(var familyId: families.keySet()) {
      var family = families.get(familyId);
      if (family.members.size() == 1 ) {
        validation.addError("Family."+familyId, "should be without familyId or have more than one familyMember: "+family.members);
      }
      var membersCount = Utils.countBy(family.members);
      for (var member: membersCount.keySet()) {
        if (!List.of("BRO", "SIS").contains(member) && membersCount.get(member) > 1) {
          validation.addError("Family."+familyId, "should have distinct familyMembers: "+family.members);
        }
      }
      if (!family.members.contains("PROBAND")) {
        validation.addError("Family."+familyId, "should have at least one PROBAND: "+family.members);
      }
    }
  }

  private void updateFamilies(Metadata.Patient patient, Map<String, Family> families) {
    if(StringUtils.isNotBlank(patient.familyId())) {
      if (!families.containsKey(patient.familyId())) {
        families.put(patient.familyId(), new Family(new ArrayList<>()));
      }
      families.get(patient.familyId()).members().add(patient.familyMember());
    }
  }

  private void validateSpecimenType(String field, Metadata m, String value, MetadataValidation validation) {
    if(StringUtils.isNoneBlank(m.submissionSchema(), value)) {
      if (SchemaValues.CQGC_Germline.name().equals(m.submissionSchema()) && !SpecimenType.NBL.name().equals(value)) {
        validation.addError(field, "'" + value + "'" + " should be: NBL for schema: "+SchemaValues.CQGC_Germline);
      }
      if (SchemaValues.CQGC_Exome_Tumeur_Seul.name().equals(m.submissionSchema()) && !SpecimenType.TUMOR.name().equals(value)) {
        validation.addError(field, "'" + value + "'" + " should be: TUMOR for schema: "+SchemaValues.CQGC_Exome_Tumeur_Seul);
      }
    }
  }

  private void validateDate(String field, String value, MetadataValidation validation, String...formats) {
    if(StringUtils.isNotBlank(value)) {
      if(!DateUtils.isValid(value, formats)) {
        validation.addError(field, "'" + value + "'" + " should be formatted like: " + String.join(" or ", formats));
      }
    }
  }

  private void validateField(String field, String value, MetadataValidation validation, List<String> values) {
    if (StringUtils.isBlank(value)) {
      if (values != null && !values.isEmpty()) {
        validation.addError(field, "should be " + values);
      } else {
        validation.addError(field, "is missing");
      }
    } else if (values != null && !values.contains(value)) {
      validation.addError(field, "'" + value + "'" + " should be " + values);
    }
  }

  private void validateRunName(String field, String value, MetadataValidation validation, String batchId, Set<String> validRunNames) {
    if (StringUtils.isNotBlank(value)) {
      if (!batchId.contains(value)) {
        validation.addWarning(field, "'" + value + "'" + " should be similar to batch_id: " + batchId);
      } else {
        validRunNames.add(value);
      }
    }
  }

  private void validateSpecialCharacters(String field, String value, MetadataValidation validation) {
    if(StringUtils.isNotBlank(value)) {
      if(StringUtils.length(value) < 2 || !NameUtils.hasNoSpecialCharacters(value)){
        validation.addError(field, "'" + value + "'" + " should have length >= 2 and no special characters, cf. regex: "+NameUtils.NO_SPECIAL_CHARACTERS);
      }
    }
  }

  private void validateRamq(String field, String value, MetadataValidation validation) {
    if (StringUtils.isNotBlank(value)) {
      if(!NameUtils.isValidRamq(value)){
        validation.addError(field, "'" + value + "'" + " should be a valid RAMQ number");
      }
    }
  }

  private void validatePatient(String field, Metadata.Patient patient, MetadataValidation validation) {
    if (StringUtils.isAllBlank(patient.mrn(), patient.ramq())) {
      validation.addError(field, "should have mrn or ramq or both");
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
        validation.addError(field, "'" + value + "'" + " should be unique");
      } else {
        previousFieldValues.add(value);
      }
    }
  }

  public List<String> extractAliquotIDs(Metadata metadata) {
    try {
      return metadata.analyses().stream().map(Metadata.Analysis::labAliquotId).filter(StringUtils::isNotBlank).distinct().sorted().toList();
    } catch (Exception e) {
      return List.of();
    }
  }

  public List<String> extractLdmServiceRequestId(Metadata metadata) {
    try {
      return metadata.analyses().stream().map(Metadata.Analysis::ldmServiceRequestId).filter(StringUtils::isNotBlank).distinct().sorted().toList();
    } catch (Exception e) {
      return List.of();
    }
  }

  public List<String> extractMRNs(Metadata metadata) {
    try {
      return metadata.analyses().stream().map(Metadata.Analysis::patient).map(Metadata.Patient::mrn).filter(StringUtils::isNotBlank).distinct().sorted().toList();
    } catch (Exception e) {
      return List.of();
    }
  }

  public List<String> extractRAMQs(Metadata metadata) {
    try {
      return metadata.analyses().stream().map(Metadata.Analysis::patient).map(Metadata.Patient::ramq).filter(StringUtils::isNotBlank).distinct().sorted().toList();
    } catch (Exception e) {
      return List.of();
    }
  }

}
