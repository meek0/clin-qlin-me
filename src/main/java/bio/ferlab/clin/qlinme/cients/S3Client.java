package bio.ferlab.clin.qlinme.cients;

import bio.ferlab.clin.qlinme.model.Metadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

@Slf4j
public class S3Client {

  private static final int MAX_KEYS = 4500;
  private static final String BACKUP_FOLDER = ".backup";
  private final software.amazon.awssdk.services.s3.S3Client s3Client;

  public S3Client(String url, String accessKey, String secretKey, int timeoutMs) {
    var s3Conf = S3Configuration.builder().pathStyleAccessEnabled(true).build();
    var s3Creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    this.s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
      .credentialsProvider(s3Creds)
      .endpointOverride(URI.create(url))
      .region(Region.US_EAST_1)
      .serviceConfiguration(s3Conf)
      .httpClientBuilder(ApacheHttpClient.builder()
        .connectionTimeout(Duration.ofMillis(timeoutMs))
        .socketTimeout(Duration.ofMillis(timeoutMs))
      ).build();
  }

  private boolean exists(String bucket, String key) {
    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  private List<String> listKeys(String bucket, String prefix) {
    return listObjects(bucket, prefix).stream().map(S3Object::key).toList();
  }

  private List<S3Object> listObjects(String bucket, String prefix) {
    var request = ListObjectsRequest.builder().bucket(bucket).prefix(prefix+"/").maxKeys(MAX_KEYS).build();
    return s3Client.listObjects(request).contents();
  }

  private void copyObject(String srcBucket, String srKey, String destBucket, String destKey) {
    log.info("Copy object: {} to {}", srKey, destKey);
    var request = CopyObjectRequest.builder().sourceBucket(srcBucket).sourceKey(srKey).destinationBucket(destBucket).destinationKey(destKey).build();
    s3Client.copyObject(request);
  }

  private void backupMetadata(String bucket, String batchId) {
    var key = batchId+ "/metadata.json";
    if(exists(bucket, key)) {
      var previous = listKeys(bucket, BACKUP_FOLDER+"/"+batchId).stream().filter(f -> !f.endsWith("latest")).toList();
      var backupKey = BACKUP_FOLDER+"/"+batchId+"/metadata.json."+(previous.size()+1);
     copyObject(bucket, key, bucket, backupKey);
    }
  }

  public List<S3Object> listBackupVersion(String bucket, String batchId) {
    return listObjects(bucket, BACKUP_FOLDER+"/"+batchId);
  }

  public void backupAndSaveMetadata(String bucket, String batchId, String metadata) {
    backupMetadata(bucket, batchId);
    var key = batchId+ "/metadata.json";
    var request = PutObjectRequest.builder().bucket(bucket).key(key).build();
    s3Client.putObject(request, RequestBody.fromString(metadata));
    copyObject(bucket, key, bucket, BACKUP_FOLDER+"/"+batchId+"/metadata.json.latest");
  }

  public byte[] getMetadata(String bucket, String batchId) throws IOException {
    var backupKey = batchId+"/metadata.json";
    var request = GetObjectRequest.builder().bucket(bucket).key(backupKey).build();
    return s3Client.getObject(request).readAllBytes();
  }

  public byte[] getBackupMetadata(String bucket, String batchId, String version) throws IOException {
    var backupKey = BACKUP_FOLDER+"/"+batchId+"/metadata.json."+version;
    var request = GetObjectRequest.builder().bucket(bucket).key(backupKey).build();
    return s3Client.getObject(request).readAllBytes();
  }

  public List<String> listBatchFiles(String bucket, String batchId) {
    return listObjects(bucket, batchId).stream().map(o -> o.key().replace(batchId+"/", ""))
      .filter(StringUtils::isNotBlank)
      .filter(f -> !f.equals("_SUCCESS"))
      .filter(f -> !f.endsWith(".md5sum"))
      .filter(f -> !f.equals("metadata.json"))
      .filter(f -> !f.endsWith(".extra_results.tgz"))
      .filter(f -> !f.endsWith(".hpo"))
      .filter(f -> !f.startsWith("logs/"))
      .toList();
  }
}
