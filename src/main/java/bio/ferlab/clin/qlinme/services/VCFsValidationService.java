package bio.ferlab.clin.qlinme.services;

import bio.ferlab.clin.qlinme.cients.S3Client;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.VCFsValidation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.*;
import java.util.zip.GZIPInputStream;

@RequiredArgsConstructor
public class VCFsValidationService {

  private final String bucket;
  private final S3Client s3Client;

  public VCFsValidation validate(Metadata m, String batchId, List<String> files){
    var validation = new VCFsValidation();
    var aliquotIDsInMetadata = extractAliquotIDs(m);
    var vcfFiles = files.stream().filter(f -> isVCF(m,f)).toList();
    var aliquotIDByVCFs = extractAliquotIDs(m, batchId, vcfFiles);

    validation.setCount(vcfFiles.size());

    for(var aliquotID : aliquotIDsInMetadata) {
      if (!aliquotIDByVCFs.containsKey(aliquotID)) {
        validation.addError(aliquotID+ " in metadata but VCF is missing");
      }
    }

    for(var aliquotID: aliquotIDByVCFs.keySet()) {
      var vcfs = aliquotIDByVCFs.get(aliquotID);
      if (!aliquotIDsInMetadata.contains(aliquotID)) {
        validation.addWarning(aliquotID+ " not related with metadata but found in VCF: "+vcfs);
      }
      if (vcfs.size() > 1) {
        validation.addWarning(aliquotID+ " has more than one VCF: "+vcfs);
      }
    }

    return validation;
  }

  private List<String> extractAliquotIDs(Metadata m) {
    var aliquotIDs = new ArrayList<String>();
    if(m!= null && m.analyses() != null) {
      for (var ana : m.analyses()) {
        if (StringUtils.isNotBlank(ana.labAliquotId())) {
          aliquotIDs.add(ana.labAliquotId());
        }
      }
    }
    return aliquotIDs;
  }

  private Map<String, List<String>> extractAliquotIDs(Metadata m, String batchId, List<String> files) {
    var aliquotIDByVCFs = new TreeMap<String, List<String>>();
    if (files != null) {
      var vcfs = files.stream().filter(f -> isVCF(m,f)).toList();
      for(var vcf: vcfs) {
        extractAliquotIDs(batchId+"/"+vcf).forEach(aliquot -> {
          aliquotIDByVCFs.computeIfAbsent(aliquot, id -> new ArrayList<>());
          aliquotIDByVCFs.get(aliquot).add(vcf);
        });
      }
    }
    return  aliquotIDByVCFs;
  }

  private List<String> extractAliquotIDs(String key) {
    ResponseInputStream<GetObjectResponse> vcfInputStream = null;
    Scanner vcfReader = null;
    var aliquotIDs = new ArrayList<String>();
    try {
      vcfInputStream = s3Client.getS3Client().getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
      vcfReader = new Scanner(new GZIPInputStream(vcfInputStream));
      while(vcfReader.hasNext() && aliquotIDs.isEmpty()) {
        var line = vcfReader.nextLine();
        if (line.startsWith("#CHROM")) {
          aliquotIDs.addAll(Arrays.stream(line.replace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t", "").split("\t")).toList());
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(vcfReader, vcfInputStream);
    }
    return aliquotIDs;
  }

  public static boolean isVCF(Metadata m, String file) {
    if (MetadataValidationService.SchemaValues.CQGC_Germline.name().equals(m.submissionSchema())) {
      return file.toLowerCase().endsWith(".hard-filtered.formatted.norm.vep.vcf.gz");
    } else if (MetadataValidationService.SchemaValues.CQGC_Exome_Tumeur_Seul.name().equals(m.submissionSchema())) {
      return file.toLowerCase().endsWith(".dragen.wes_somatic-tumor_only.hard-filtered.norm.vep.vcf.gz");
    }
    return false;
  }
}
