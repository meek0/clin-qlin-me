package bio.ferlab.clin.qlinme.services;

import bio.ferlab.clin.qlinme.TestUtils;
import bio.ferlab.clin.qlinme.cients.S3Client;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import bio.ferlab.clin.qlinme.model.VCFsValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class VCFsValidationServiceTest {

  final software.amazon.awssdk.services.s3.S3Client baseS3Client = Mockito.mock(software.amazon.awssdk.services.s3.S3Client.class);
  final S3Client s3Client = Mockito.mock(S3Client.class);
  final VCFsValidationService service = new VCFsValidationService("input", s3Client);

  @BeforeEach
  void beforeEach() throws IOException {

    // only works for allowCache = true
    // should use container test for a real S3 exchange

    when(s3Client.getS3Client()).thenReturn(baseS3Client);

    HeadObjectResponse headObjectResponse = Mockito.mock(HeadObjectResponse.class);
    when(headObjectResponse.lastModified()).thenReturn(Instant.ofEpochSecond(1));
    when(baseS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);

    when(s3Client.getCachedVCFAliquotIDs(any(), any(), any())).thenReturn(new byte[]{});
    when(s3Client.getCachedVCFAliquotIDs(any(), eq("invalid_vcfs_germline/00001.hard-filtered.formatted.norm.vep.vcf.gz"), any())).thenReturn("00001".getBytes());
    when(s3Client.getCachedVCFAliquotIDs(any(), eq("invalid_vcfs_germline/00002.hard-filtered.formatted.norm.vep.vcf.gz"), any())).thenReturn("00002,00003".getBytes());
  }

  @Test
  void invalid_vcfs_germline() {
    assertBatchId("invalid_vcfs_germline", List.of("00001.hard-filtered.formatted.norm.vep.vcf.gz","00002.hard-filtered.formatted.norm.vep.vcf.gz"), true);
  }

  private VCFsValidation assertBatchId(String batchId, List<String> files, boolean allowCache) {
    var metadata = TestUtils.loadTestMetadata(batchId);
    var validation = service.validate(metadata, batchId, files, allowCache);
    TestUtils.assertValidation(batchId, validation);
    return validation;
  }

}
