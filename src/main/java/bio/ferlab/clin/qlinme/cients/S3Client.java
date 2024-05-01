package bio.ferlab.clin.qlinme.cients;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class S3Client {

  private static final int MAX_KEYS = 1000;
  private static final String BACKUP_FOLDER = ".backup";
  private static final String CACHE_FOLDER = ".cache";

  @Getter
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
    List<S3Object> s3Objects = new ArrayList<>();
    ListObjectsV2Response lastResult = null;
    while(lastResult == null || lastResult.isTruncated()) {
      var request = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix+"/").maxKeys(MAX_KEYS);
      if (lastResult !=null && lastResult.isTruncated()) {
        request.continuationToken(lastResult.nextContinuationToken());
      }
      lastResult = s3Client.listObjectsV2(request.build());
      s3Objects.addAll(lastResult.contents());
    }
    return s3Objects;
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
    return readAndClose(bucket, backupKey);
  }

  public byte[] getBackupMetadata(String bucket, String batchId, String version) throws IOException {
    var backupKey = BACKUP_FOLDER+"/"+batchId+"/metadata.json."+version;
    return readAndClose(bucket, backupKey);
  }

  public byte[] getCachedVCFAliquotIDs(String bucket, String key, Instant lastModified) throws IOException {
    var cachedKey = formatCachedVCFAliquotIDsKey(key, lastModified.toString());
    return readAndClose(bucket, cachedKey);
  }

  private byte[] readAndClose(String bucket, String key) throws IOException {
    var request = GetObjectRequest.builder().bucket(bucket).key(key).build();
    var response = s3Client.getObject(request);
    var data = response.readAllBytes();
    response.close();
    return data;
  }

  public void setCachedVCFAliquotIDs(String bucket, String key, Instant lastModified, List<String> ids) throws IOException {
    var backupKey = formatCachedVCFAliquotIDsKey(key, lastModified.toString());
    var request = PutObjectRequest.builder().bucket(bucket).key(backupKey).build();
    s3Client.putObject(request, RequestBody.fromString(String.join(",", ids)));
  }

  private String formatCachedVCFAliquotIDsKey(String key, String lastModified) {
    return CACHE_FOLDER+"/"+key+"."+lastModified+".aliquots";
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
      .filter(f -> !f.toLowerCase().endsWith(".norm.vep.vcf.gz.tbi"))
      .toList();
  }

}
