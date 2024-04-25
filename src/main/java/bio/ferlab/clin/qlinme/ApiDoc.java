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
        .withDocumentationPath(deprecatedDocsPath)
        //.withRoles(App.Roles.ANONYMOUS)
        /*.withDefinitionConfiguration((version, openApiDefinition) ->
          openApiDefinition
            .withInfo(openApiInfo ->
              openApiInfo
                .title("Qlin-Me")
                .summary("API to import data with validations")
            )
            .withServer(openApiServer ->
              openApiServer
                .description("Server endpoint")
                .url(serverUrl)
            )
            .withSecurity(openApiSecurity ->
              openApiSecurity
                .withBasicAuth()
                .withBearerAuth()
                .withApiKeyAuth("ApiKeyAuth", "X-Api-Key")
                .withCookieAuth("CookieAuth", "JSESSIONID")
                .withOpenID("OpenID", "https://example.com/.well-known/openid-configuration")
                .withOAuth2("OAuth2", "This API uses OAuth 2 with the implicit grant flow.", oauth2 ->
                  oauth2
                    .withClientCredentials("https://api.example.com/credentials/authorize")
                    .withImplicitFlow("https://api.example.com/oauth2/authorize", flow ->
                      flow
                        .withScope("read_pets", "read your pets")
                        .withScope("write_pets", "modify pets in your account")
                    )
                )
                .withGlobalSecurity("OAuth2", globalSecurity ->
                  globalSecurity
                    .withScope("write_pets")
                    .withScope("read_pets")
                )
            )
            .withDefinitionProcessor(content -> { // you can add whatever you want to this document using your favourite json api
              content.set("test", new TextNode("Value"));
              return content.toPrettyString();
            })
        )*/));

    config.registerPlugin(new ReDocPlugin(reDocConfiguration -> {
      reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
      reDocConfiguration.setUiPath("/");
      reDocConfiguration.setRoles(new RouteRole[]{App.Roles.ANONYMOUS});
    }));

    log.info("OpenAPI doc: {}", deprecatedDocsPath);
  }
}
