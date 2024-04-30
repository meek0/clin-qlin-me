package bio.ferlab.clin.qlinme;

import bio.ferlab.clin.qlinme.cients.FhirClient;
import bio.ferlab.clin.qlinme.cients.KeycloakClient;
import bio.ferlab.clin.qlinme.cients.S3Client;
import bio.ferlab.clin.qlinme.controllers.AuthController;
import bio.ferlab.clin.qlinme.controllers.BatchController;
import bio.ferlab.clin.qlinme.handlers.ExceptionHandler;
import bio.ferlab.clin.qlinme.handlers.HealthCheckHandler;
import bio.ferlab.clin.qlinme.handlers.SecurityHandler;
import bio.ferlab.clin.qlinme.handlers.Slf4jRequestLogger;
import bio.ferlab.clin.qlinme.model.FilesValidation;
import bio.ferlab.clin.qlinme.model.VCFsValidation;
import bio.ferlab.clin.qlinme.services.FilesValidationService;
import bio.ferlab.clin.qlinme.services.MetadataValidationService;
import bio.ferlab.clin.qlinme.services.VCFsValidationService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import io.javalin.security.RouteRole;
import io.javalin.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

  // all singletons
  public static final Config config = new Config();
  public static final JavalinJackson objectMapper = new JavalinJackson().updateMapper(mapper -> {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
  });
  public static final HealthCheckHandler healthCheckHandler = new HealthCheckHandler();
  public static final Slf4jRequestLogger requestLogger = new Slf4jRequestLogger();
  public static final ExceptionHandler exceptionHandler = new ExceptionHandler();
  public static final S3Client s3Client = new S3Client(config.awsEndpoint, config.awsAccessKey, config.awsSecretKey, 15000);
  //public static final FhirClient fhirClient = new FhirClient(config.fhirUrl, 15000, 20);
  public static final KeycloakClient keycloakClient = new KeycloakClient(config.securityIssuer, config.securityClient, config.securityAudience, 15000);
  public static final SecurityHandler securityHandler = new SecurityHandler(config.securityIssuer, config.securityAudience, config.securityEnabled);
  public static final AuthController authController = new AuthController(keycloakClient);
  public static final MetadataValidationService metadataValidationService = new MetadataValidationService();
  public static final FilesValidationService filesValidationService = new FilesValidationService();
  public static final VCFsValidationService vcfsValidationService = new VCFsValidationService(config.awsBucket, s3Client);
  public static final BatchController batchController = new BatchController(s3Client, config.awsBucket, metadataValidationService, filesValidationService, vcfsValidationService, objectMapper);

  public static void main(String[] args) {
    var app = Javalin.create(conf -> {
        conf.useVirtualThreads = true;
        conf.showJavalinBanner = false;
        conf.requestLogger.http(requestLogger);
        conf.http.gzipOnlyCompression();
        conf.jsonMapper(objectMapper);
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
      .exception(ValidationException.class, exceptionHandler::handle)
      .exception(Exception.class, exceptionHandler::handle)
      .beforeMatched(ctx -> {
        if (!ctx.routeRoles().contains(Roles.ANONYMOUS) && !Utils.isPublicRoute(ctx)) {
          securityHandler.checkAuthorization(ctx);
        }
      })
      .get(Routes.ACTUATOR_HEALTH, healthCheckHandler)
      .get(Routes.AUTH_LOGIN, authController::login, Roles.ANONYMOUS)
      .get(Routes.BATCH, batchController::batchRead, Roles.USER)
      .post(Routes.BATCH, batchController::batchCreateUpdate, Roles.USER)
      .get(Routes.BATCH_STATUS, batchController::batchStatus, Roles.USER)
      .get(Routes.BATCH_HISTORY, batchController::batchHistory, Roles.USER)
      .get(Routes.BATCH_HISTORY_BY_VERSION, batchController::batchHistoryByVersion, Roles.USER)
      .start(config.port);
  }

  enum Roles implements RouteRole {
    ANONYMOUS,
    USER,
  }

}
