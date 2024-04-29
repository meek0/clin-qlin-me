package bio.ferlab.clin.qlinme.services;

import bio.ferlab.clin.qlinme.model.FilesValidation;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FilesValidationService {

  public FilesValidation validateFiles(Metadata m, List<String> files) {
    var validation = new FilesValidation();
    var all = new ArrayList<String>();
    if (files != null && !files.isEmpty()) {
      log.debug("Files in S3: {}", files.size());
      validation.setFilesCount(files.size());
      if (m.analyses() != null) {
        for (int ai = 0 ; ai < m.analyses().size() ; ai ++) {
          var ana = m.analyses().get(ai);
          //var errorPrefix = "analyses["+ai+"]";
          if (ana.files() != null) {
            for (String fileKey : MetadataValidationService.fileKeys) {
              validateFile(ana.files().get(fileKey), files, validation, all);
            }
          }
        }
      }
      for(var file : files) {
        if(!all.contains(file)) {
          if ("CQGC_Germline".equals(m.submissionSchema())) {
            if (!file.toLowerCase().endsWith(".hard-filtered.formatted.norm.vep.vcf.gz")) {
              validation.addError(file + " not in metadata");
            }else {
              validation.setVcfsCount(validation.getVcfsCount()+1);
            }
          } else if ("CQGC_Exome_Tumeur_Seul".equals(m.submissionSchema())) {
            if (!file.toLowerCase().endsWith(".dragen.wes_somatic-tumor_only.hard-filtered.norm.vep.vcf.gz")) {
              validation.addError(file + " not in metadata");
            }else {
              validation.setVcfsCount(validation.getVcfsCount()+1);
            }
          }
        }
      }
    } else {
      validation.addError("Files are missing");
    }
    return validation;
  }

  private void validateFile(String file, List<String> files, FilesValidation validation, List<String> all) {
    if(StringUtils.isNotBlank(file)) {
      all.add(file);
      if(!files.contains(file)) {
        validation.addError(String.format("%s is missing", file));
      }
    }
  }
}
