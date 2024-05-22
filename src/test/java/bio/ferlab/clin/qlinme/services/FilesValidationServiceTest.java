package bio.ferlab.clin.qlinme.services;

import bio.ferlab.clin.qlinme.TestUtils;
import bio.ferlab.clin.qlinme.model.FilesValidation;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilesValidationServiceTest {

  final FilesValidationService service = new FilesValidationService();

  @Test
  void missing_files() {
    assertBatchId("missing_files", null);
  }

  @Test
  void invalid_files() {
    assertBatchId("invalid_files", List.of("01.cram"));
  }

  private FilesValidation assertBatchId(String batchId, List<String> files) {
    var metadata = TestUtils.loadTestMetadata(batchId);
    var validation = service.validateFiles(metadata, files);
    TestUtils.assertValidation(batchId, validation);
    return validation;
  }
}
