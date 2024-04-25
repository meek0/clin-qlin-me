package bio.ferlab.clin.qlinme;

import io.javalin.config.JavalinConfig;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.security.RouteRole;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiDoc {

  public static final String deprecatedDocsPath = "/api/openapi.json";

  public static void create(JavalinConfig config) {
    config.registerPlugin(new OpenApiPlugin(openApiConfig ->
      openApiConfig
        .withDocumentationPath(deprecatedDocsPath)));

    config.registerPlugin(new ReDocPlugin(reDocConfiguration -> {
      reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
      reDocConfiguration.setUiPath("/");
      reDocConfiguration.setRoles(new RouteRole[]{App.Roles.ANONYMOUS});
    }));

    log.info("OpenAPI doc: {}", deprecatedDocsPath);
  }
}
