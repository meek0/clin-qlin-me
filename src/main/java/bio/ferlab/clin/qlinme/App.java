package bio.ferlab.clin.qlinme;

import bio.ferlab.clin.qlinme.cients.FhirClient;
import bio.ferlab.clin.qlinme.cients.KeycloakClient;
import bio.ferlab.clin.qlinme.cients.S3Client;
import bio.ferlab.clin.qlinme.controllers.AuthController;
import bio.ferlab.clin.qlinme.controllers.HelloController;
import bio.ferlab.clin.qlinme.handlers.ExceptionHandler;
import bio.ferlab.clin.qlinme.handlers.HealthCheckHandler;
import bio.ferlab.clin.qlinme.handlers.Slf4jRequestLogger;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.json.JavalinJackson;
import io.javalin.security.RouteRole;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

  // all singletons
  public static final Config config = new Config();
  public static final HealthCheckHandler healthCheckHandler = new HealthCheckHandler();
  public static final Slf4jRequestLogger requestLogger = new Slf4jRequestLogger();
  public static final ExceptionHandler exceptionHandler = new ExceptionHandler();
  public static final HelloController helloController = new HelloController();
  public static final S3Client s3Client = new S3Client(config.awsEndpoint, config.awsAccessKey, config.awsSecretKey, 15000);
  public static final FhirClient fhirClient = new FhirClient(config.fhirUrl, 15000, 20);
  public static final KeycloakClient keycloakClient = new KeycloakClient(config.keycloakUrl, config.keycloakRealm, 15000);
  public static final AuthController authController = new AuthController(keycloakClient);

  public static void main(String[] args) {
    var app = Javalin.create(conf -> {
        conf.useVirtualThreads = true;
        conf.showJavalinBanner = false;
        conf.requestLogger.http(requestLogger);
        conf.http.gzipOnlyCompression();
        conf.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
          mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
          //mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        }));
        conf.events(event -> {
          event.serverStarting(() -> {
            log.info("App is starting with Cores: {}", Runtime.getRuntime().availableProcessors());
            log.info("Current profile: {}", config.env);
          });
          event.serverStarted(() -> {
            log.info("HTTP server started on port: " + config.port);
            log.info("App started in {}ms", System.currentTimeMillis() - config.start);
          });
        });
        ApiDoc.create(conf);
      })
      .exception(HttpResponseException.class, exceptionHandler::handle)
      .exception(Exception.class, exceptionHandler::handle)
      .get(Routes.ACTUATOR_HEALTH, healthCheckHandler)
      .get(Routes.AUTH_LOGIN, authController::login)
      .get("/api/v1/hello", helloController::hello)
      .post("/api/v1/body", helloController::body)
      .start(config.port);
  }

  enum Roles implements RouteRole {
    ANONYMOUS,
    USER,
  }

}
