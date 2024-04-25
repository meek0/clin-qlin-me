package bio.ferlab.clin.qlinme.handlers;

import bio.ferlab.clin.qlinme.App;
import io.javalin.http.Context;
import io.javalin.http.RequestLogger;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class Slf4jRequestLogger implements RequestLogger {

  public void handle(@NotNull Context ctx, @NotNull Float ms) {
    if (App.config.publics.stream().noneMatch(p -> ctx.req().getRequestURI().startsWith(p))) {
      log.info("{} {} {} in {} ms", ctx.req().getMethod(), ctx.res().getStatus(), ctx.req().getRequestURI(), ms);
    }
  }
}
