package bio.ferlab.clin.qlinme.handlers;

import bio.ferlab.clin.qlinme.utils.Utils;
import io.javalin.http.Context;
import io.javalin.http.RequestLogger;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class Slf4jRequestLogger implements RequestLogger {

  public void handle(@NotNull Context ctx, @NotNull Float ms) {
    if (!Utils.isPublicRoute(ctx)) {
      log.info("{} {} {} in {} ms", ctx.req().getMethod(), ctx.res().getStatus(), ctx.req().getRequestURI(), ms);
    }
  }
}
