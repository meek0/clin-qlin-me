package bio.ferlab.clin.qlinme.services;

import bio.ferlab.clin.qlinme.cients.S3Client;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.VCFsValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.time.Instant;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Slf4j
@RequiredArgsConstructor
public class VCFsValidationService {

  private final String bucket;
  private final S3Client s3Client;

  public VCFsValidation validate(Metadata m, String batchId, List<String> files, boolean allowCache){
    var validation = new VCFsValidation();
    var aliquotIDsInMetadata = extractAliquotIDs(m);
    var vcfFiles = files.stream().filter(f -> isVCF(m,f)).toList();
    var aliquotIDByVCFs = extractAliquotIDs(m, batchId, vcfFiles, allowCache);

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

  private Map<String, List<String>> extractAliquotIDs(Metadata m, String batchId, List<String> files, boolean allowCache) {
    var aliquotIDByVCFs = new TreeMap<String, List<String>>();
    if (files != null) {
      var vcfs = files.stream().filter(f -> isVCF(m,f)).toList();
      for(var vcf: vcfs) {
        extractAliquotIDs(batchId+"/"+vcf, allowCache).forEach(aliquot -> {
          aliquotIDByVCFs.computeIfAbsent(aliquot, id -> new ArrayList<>());
          aliquotIDByVCFs.get(aliquot).add(vcf);
        });
      }
    }
    return  aliquotIDByVCFs;
  }

  private List<String> extractAliquotIDs(String key, boolean allowCache) {
    ResponseInputStream<GetObjectResponse> vcfInputStream = null;
    Scanner vcfReader = null;
    var aliquotIDs = new ArrayList<String>();
    Instant lastModified = null;
    try {
      lastModified = s3Client.getS3Client().headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).lastModified();
      var cached = extractAliquotIDsFromCache(key, lastModified, allowCache);
      if (cached.isPresent()) {
        aliquotIDs.addAll(cached.get());
      } else {
        vcfInputStream = s3Client.getS3Client().getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        lastModified = vcfInputStream.response().lastModified();
        vcfReader = new Scanner(new GZIPInputStream(vcfInputStream));
        while(vcfReader.hasNext() && aliquotIDs.isEmpty()) {
          var line = vcfReader.nextLine();
          if (line.startsWith("#CHROM")) {
            aliquotIDs.addAll(Arrays.stream(line.replace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t", "").split("\t")).toList());
            vcfInputStream.abort(); // ignore the remaining response
          }
        }
      }
      if (aliquotIDs.isEmpty()) {
        throw new RuntimeException("No aliquots IDs found in: "+ key);
      }
      s3Client.setCachedVCFAliquotIDs(bucket, key, lastModified, aliquotIDs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(vcfReader, vcfInputStream);
    }
    return aliquotIDs;
  }


  private Optional<List<String>> extractAliquotIDsFromCache(String key, Instant lastModified, boolean allowCache) {
    try {
      if (!allowCache) return Optional.empty();
      var data = s3Client.getCachedVCFAliquotIDs(bucket, key, lastModified);
      log.info("VCF aliquot IDs from cache: {} {}", key, lastModified);
      return Optional.of(Arrays.stream(new String(data).split(",")).toList());
    } catch (NoSuchKeyException e) {
      return Optional.empty();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
