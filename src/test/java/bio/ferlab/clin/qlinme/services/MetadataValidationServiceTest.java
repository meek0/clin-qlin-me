package bio.ferlab.clin.qlinme.services;

import bio.ferlab.clin.qlinme.TestUtils;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class MetadataValidationServiceTest {

  final MetadataValidationService service = new MetadataValidationService();
  final List<String> panelCodes = List.of("RGDI", "MMG");
  final List<String> organizations = List.of("CHUSJ", "LDM-CHUSJ", "CUSM", "LDM-CUSM");
  final Map<String, List<String>> aliquotIDsByBatch = Map.of("another_batch", List.of("16900"));
  final List<Metadata.Patient> patients = List.of(new Metadata.Patient("First Name", "Last Name", "MALE", "LASF11112222", "12/08/1981", "MRN-00001", "CHUSJ", null, null, null, null)); @Test
  void empty() {
    assertBatchId("empty");
  }

  @Test
  void missing_fields() {
    assertBatchId("missing_fields");
  }

  @Test
  void blank_fields() {
    assertBatchId("blank_fields");
  }

  @Test
  void null_fields() {
    assertBatchId("null_fields");
  }

  @Test
  void invalid_values() {
    assertBatchId("invalid_values");
  }

  @Test
  void unique_values() {
    assertBatchId("unique_values");
  }

  @Test
  void related_fields_germline() {
    assertBatchId("related_fields_germline");
  }

  @Test
  void related_fields_somatic() {
    assertBatchId("related_fields_somatic");
  }

  @Test
  void valid() {
    assertBatchId("valid");
  }

  @Test
  void unique_aliquots() {
    assertBatchId("unique_aliquots");
  }

  @Test
  void invalid_patient() {
    assertBatchId("invalid_patient");
  }

  private MetadataValidation assertBatchId(String batchId) {
    var metadata = TestUtils.loadTestMetadata(batchId);
    var validation = service.validateMetadata(metadata, batchId, panelCodes, organizations, aliquotIDsByBatch, patients);
    TestUtils.assertValidation(batchId, validation);
    return validation;
  }

}
