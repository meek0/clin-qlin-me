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
import bio.ferlab.clin.qlinme.services.FilesValidationService;
import bio.ferlab.clin.qlinme.services.MetadataValidationService;
import bio.ferlab.clin.qlinme.services.VCFsValidationService;
import bio.ferlab.clin.qlinme.utils.S3TimedCache;
import bio.ferlab.clin.qlinme.utils.Utils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import io.javalin.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

  public static final Config CONFIG = new Config();

  public static void main(String[] args) {
    // all singletons
    final JavalinJackson objectMapper = new JavalinJackson().updateMapper(mapper -> {
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      //mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    });
    final HealthCheckHandler healthCheckHandler = new HealthCheckHandler();
    final Slf4jRequestLogger requestLogger = new Slf4jRequestLogger();
    final ExceptionHandler exceptionHandler = new ExceptionHandler();
    final S3Client s3Client = new S3Client(CONFIG.awsEndpoint, CONFIG.awsAccessKey, CONFIG.awsSecretKey, 15000);
    final KeycloakClient keycloakClient = new KeycloakClient(CONFIG.securityIssuer, CONFIG.securityClient, CONFIG.securityAudience, 15000);
    final SecurityHandler securityHandler = new SecurityHandler(CONFIG.securityIssuer, CONFIG.securityAudience);
    final S3TimedCache fhirCache = new S3TimedCache(s3Client, CONFIG.awsBucket, objectMapper.getMapper(), CONFIG.fhirCacheInHour);
    final FhirClient fhirClient = new FhirClient(CONFIG.fhirUrl, 15000, 20, fhirCache);
    final AuthController authController = new AuthController(keycloakClient);
    final MetadataValidationService metadataValidationService = new MetadataValidationService();
    final FilesValidationService filesValidationService = new FilesValidationService();
    final VCFsValidationService vcfsValidationService = new VCFsValidationService(CONFIG.awsBucket, s3Client);
    final BatchController batchController = new BatchController(s3Client, CONFIG.awsBucket, metadataValidationService, filesValidationService, vcfsValidationService, objectMapper, fhirClient);

    var app = Javalin.create(conf -> {
        conf.useVirtualThreads = true;
        conf.showJavalinBanner = false;
        conf.requestLogger.http(requestLogger);
        conf.http.gzipOnlyCompression();
        conf.jsonMapper(objectMapper);
        conf.events(event -> {
          event.serverStarting(() -> {
            log.info("App is starting with Cores: {}", Runtime.getRuntime().availableProcessors());
            log.info("Current log level: {}", CONFIG.logLevel);
          });
          event.serverStarted(() -> {
            log.info("HTTP server started on port: " + CONFIG.port);
            log.info("App started in {}ms", System.currentTimeMillis() - CONFIG.start);
          });
        });
        ApiDoc.create(conf);
      })
      .exception(HttpResponseException.class, exceptionHandler::handle)
      .exception(ValidationException.class, exceptionHandler::handle)
      .exception(Exception.class, exceptionHandler::handle)
      .beforeMatched(ctx -> {
        if(CONFIG.securityEnabled) {
          if (!ctx.routeRoles().contains(SecurityHandler.Roles.anonymous) && !Utils.isPublicRoute(ctx)) {
            var roles = securityHandler.checkAuthorization(ctx);
            if (ctx.routeRoles().stream().noneMatch(roles::contains)) {
              throw new HttpResponseException(HttpStatus.FORBIDDEN.getCode(), "insufficient access rights");
            }
          }
        }
      })
      .get(Routes.ACTUATOR_HEALTH, healthCheckHandler) /* health is public */
      .get(Routes.AUTH_LOGIN, authController::login, SecurityHandler.Roles.anonymous)
      .get(Routes.BATCH, batchController::batchRead, SecurityHandler.Roles.clin_qlin_me)
      .post(Routes.BATCH, batchController::batchCreateUpdate, SecurityHandler.Roles.clin_qlin_me)
      .get(Routes.BATCH_STATUS, batchController::batchStatus, SecurityHandler.Roles.clin_qlin_me)
      .get(Routes.BATCH_HISTORY, batchController::batchHistory, SecurityHandler.Roles.clin_qlin_me)
      .get(Routes.BATCH_HISTORY_BY_VERSION, batchController::batchHistoryByVersion, SecurityHandler.Roles.clin_qlin_me)
      .start(CONFIG.port);

    Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
  }



}
