package bio.ferlab.clin.qlinme.model;

import java.util.List;
import java.util.Map;

public record Metadata(String submissionSchema, List<Analysis> analyses) {
  public record Analysis(String ldm, String ldmSampleId, String ldmSpecimenId, String specimenType, String sampleType,
                         String ldmServiceRequestId, String labAliquotId, Patient patient, Map<String, String> files,
                         String analysisCode, String panelCode, Experiment experiment, Workflow workflow) {
  }

  public record Patient(String firstName, String lastName, String sex, String ramq, String birthDate, String mrn,
                        String ep,
                        String designFamily, String familyMember, String familyId, String status, boolean fetus) {
  }

  public record Experiment(String platform, String sequencerId, String runName, String runDate, String runAlias,
                           String flowcellId, boolean isPairedEnd, int fragmentSize, String experimentalStrategy,
                           String captureKit, String baitDefinition, String protocol) {
  }

  public record Workflow(String name, String version, String genomeBuild, String genomeBuildVersion,
                         String vepVersion) {
  }
}
