package bio.ferlab.clin.qlinme.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TimedCache<K, V> {

  protected Map<K, CacheValue<V>> cacheMap;
  protected int cacheTimeout;

  public TimedCache(int cacheTimeoutSec) {
    this.cacheTimeout = cacheTimeoutSec;
    this.clear();
  }

  private boolean isExpired(K key) {
    if (this.cacheMap.containsKey(key)) {
      LocalDateTime expirationDateTime = this.cacheMap.get(key).getCreatedAt().plusSeconds(this.cacheTimeout);
      return LocalDateTime.now().isAfter(expirationDateTime);
    }
    return false;
  }

  public void clear() {
    this.cacheMap = new ConcurrentHashMap<>();
  }

  public Optional<V> get(K key) {
    if (isExpired(key)) {
      remove(key);
      return Optional.empty();
    } else {
      return Optional.ofNullable(this.cacheMap.get(key)).map(CacheValue::getValue);
    }
  }

  public V put(K key, V value) {
    this.cacheMap.put(key, this.createCacheValue(value));
    return value;
  }

  protected CacheValue<V> createCacheValue(V value) {
    LocalDateTime now = LocalDateTime.now();
    return new CacheValue<V>() {
      @Override
      public V getValue() {
        return value;
      }

      @Override
      public LocalDateTime getCreatedAt() {
        return now;
      }
    };
  }

  public void remove(K key) {
    this.cacheMap.remove(key);
  }

  protected interface CacheValue<V> {
    V getValue();

    LocalDateTime getCreatedAt();
  }

}
