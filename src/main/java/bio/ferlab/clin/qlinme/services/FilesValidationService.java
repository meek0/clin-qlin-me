package bio.ferlab.clin.qlinme.services;

import bio.ferlab.clin.qlinme.model.FilesValidation;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class FilesValidationService {

  public FilesValidation validateFiles(Metadata m, List<String> files) {
    var validation = new FilesValidation();
    var all = new ArrayList<String>();
    if (files != null && !files.isEmpty()) {
      validation.setCount(files.size());
      if (m.analyses() != null) {
        for (int ai = 0 ; ai < m.analyses().size() ; ai ++) {
          var ana = m.analyses().get(ai);
          if (ana.files() != null) {
            for (var fileKey : MetadataValidationService.Files.values()) {
              validateFile(ana.files().get(fileKey.name()), files, validation, all);
            }
          }
        }
      }
      for(var file : files) {
        if(!all.contains(file)) {
          if (!VCFsValidationService.isVCF(m, file)) {
            validation.addError(file + " not in metadata");
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
