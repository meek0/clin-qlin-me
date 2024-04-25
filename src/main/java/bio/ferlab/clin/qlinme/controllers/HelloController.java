package bio.ferlab.clin.qlinme.controllers;

import bio.ferlab.clin.qlinme.model.Body;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;

public class HelloController {

  @OpenApi(
    summary = "Just a Hello",
    operationId = "hello",
    path = "/hello",
    methods = HttpMethod.GET,
    queryParams = {
      @OpenApiParam(name = "email", required = true),
      @OpenApiParam(name = "password", required = true),
    },
    tags = {"User"},
    responses = {
      @OpenApiResponse(status = "200"),
    }
  )
  public void hello(Context ctx) {
    ctx.result("Hello World!");
  }

  public void body(Context ctx) {
    ctx.bodyValidator(Body.class)
      .check(b -> !b.name().isEmpty(), "name cant be empty")
      .check(b -> b.value() != null && b.value() > 0, "value should be bigegr than 0")
      .get();
    ctx.result("OK");
  }
}
