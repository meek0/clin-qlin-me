package bio.ferlab.clin.qlinme.model;

import java.util.List;

public record Metadata(String submissionSchema, List<Analysis> analyses) {
  public record Analysis(String ldm, String ldmSampleId, String ldmSpecimenId, String specimenType, String sampleType,
                         String ldmServiceRequestId, String labAliquotId, Patient patient, Files files,
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

  public record Files(String cram, String crai, String snv_vcf, String snv_tbi, String cnv_vcf, String cnv_tbi,
                      String sv_vcf, String sv_tbi, String supplement,
                      String exomiser_html, String exomiser_json, String exomiser_variants_tsv, String seg_bw,
                      String hard_filtered_baf_bw,
                      String roh_bed, String hyper_exome_hg38_bed, String cnv_calls_png, String coverage_by_gene_csv,
                      String qc_metrics) {
  }
}
