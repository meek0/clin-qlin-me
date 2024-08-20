package bio.ferlab.clin.qlinme.utils;

import bio.ferlab.clin.qlinme.cients.S3Client;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class S3TimedCache {

  private final S3Client s3Client;
  private final String bucket;
  private final ObjectMapper objectMapper;
  private final int cacheTimeoutInHour;

  private boolean isExpired(String key) {
    try {
      var lastModified = s3Client.getS3Client().headObject(HeadObjectRequest.builder().bucket(bucket).key(buildCacheKey(key)).build()).lastModified();
      var expirationDateTime = lastModified.plus(this.cacheTimeoutInHour, ChronoUnit.HOURS);
      return Instant.now().isAfter(expirationDateTime);
    } catch (NoSuchKeyException e) {
      return true;
    }
  }

  public <T> Optional<T> get(String key, TypeReference<T> t) {
    if (isExpired(key)) {
      return Optional.empty();
    } else {
      try {
        return Optional.of(objectMapper.readValue(s3Client.getS3Client().getObject(GetObjectRequest.builder().bucket(bucket).key(buildCacheKey(key)).build()), t));
      } catch (Exception e) {
        log.warn("Invalidate cache: {} cause: {}", key, e.getMessage());
        this.s3Client.getS3Client().deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(buildCacheKey(key)).build());
        return Optional.empty();
      }
    }
  }

  public <T> T put(String key, T value) {
    try {
    s3Client.getS3Client().putObject(PutObjectRequest.builder().bucket(bucket).key(buildCacheKey(key)).build(), RequestBody.fromString(objectMapper.writeValueAsString(value)));
    return value;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String buildCacheKey(String key) {
    return S3Client.CACHE_FOLDER+"/"+key;
  }
}
