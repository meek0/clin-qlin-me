package bio.ferlab.clin.qlinme;

import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class Config {

  public final long start = System.currentTimeMillis();
  public final List<String> publics = List.of("/actuator/health", "/favicon.ico", "/webjars", "/api/openapi.json");
  public final Integer port = getEnv("PORT").map(Integer::parseInt).orElse(7979);
  public final Env env = getEnv("ENV").map(Env::valueOf).orElse(Env.prod);

  public final String awsAccessKey = getEnv("AWS_ACCESS_KEY").orElse("minio");
  public final String awsSecretKey = getEnv("AWS_SECRET_KEY").orElse(("minio123"));
  public final String awsEndpoint = getEnv("AWS_ENDPOINT").orElse("http://localhost:9000");

  public final String fhirUrl = getEnv("FHIR_URL").orElse(" https://fhir.qa.cqgc.hsj.rtss.qc.ca/fhir");

  public final String keycloakUrl = getEnv("KEYCLOAK_URL").orElse("https://auth.qa.cqgc.hsj.rtss.qc.ca");
  public final String keycloakRealm = getEnv("KEYCLOAK_REALM").orElse("clin");
  public final String keycloakClient = getEnv("KEYCLOAK_CLIENT").orElse("clin-client");
  public final String keycloakAudience = getEnv("KEYCLOAK_AUDIENCE").orElse("clin-acl");

  public final String serverBaseUrl = getEnv("SERVER_BASE_URL").orElse("http://localhost:8080");

  public Config() {
    var rootLogger = ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME));
    rootLogger.setLevel(Env.prod.equals(env) ? Level.ERROR : Level.INFO);
  }

  private Optional<String> getEnv(String name) {
    return Optional.ofNullable(System.getenv(name));
  }

  private String getRequiredEnv(String name) {
    return Optional.ofNullable(System.getenv(name)).orElseThrow(() -> new IllegalStateException("Missing env var: " + name));
  }

  enum Env {
    qa,
    prod
  }
}
