package bio.ferlab.clin.qlinme.controllers;

import bio.ferlab.clin.qlinme.Routes;
import bio.ferlab.clin.qlinme.Utils;
import bio.ferlab.clin.qlinme.cients.KeycloakClient;
import bio.ferlab.clin.qlinme.model.UserToken;
import com.auth0.jwt.JWT;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import io.javalin.validation.Validation;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class AuthController {

  private final KeycloakClient keycloakClient;

  @OpenApi(
    summary = "Basic auth",
    description = "Return an access token for the user from an email and password",
    operationId = "authLoginByEmailPassword",
    path = Routes.AUTH_LOGIN,
    methods = HttpMethod.GET,
    tags = {"Authentication"},
    queryParams = {
      @OpenApiParam(name = "email", required = true),
      @OpenApiParam(name = "password", required = true),
    },
    responses = {
      @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserToken.class)),
      @OpenApiResponse(status = "400"),
      @OpenApiResponse(status = "401"),
    }
  )
  public void login(Context ctx) throws IOException {
    var email = Utils.getValidQueryParam(ctx, "email");
    var password = Utils.getValidQueryParam(ctx, "password");
    var errors = Validation.collectErrors(email, password);
    if (!errors.isEmpty()) {
      ctx.status(HttpStatus.BAD_REQUEST).json(errors);
    } else {
      var token = keycloakClient.getRpt(keycloakClient.getAccessToken(email.get(), password.get()));
      var decoded = JWT.decode(token);
      var expiresAt = decoded.getClaim("exp").asInt();
      var duration = expiresAt - decoded.getClaim("iat").asInt();
      ctx.json(new UserToken(token, expiresAt, duration));
    }
  }
}
