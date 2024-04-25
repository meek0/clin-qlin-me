package bio.ferlab.clin.qlinme.handlers;

import bio.ferlab.clin.qlinme.model.HealthStatus;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class HealthCheckHandler implements Handler {

  private final HealthStatus healthStatusOK = new HealthStatus("OK");

  @Override
  public void handle(@NotNull Context ctx) {
    ctx.json(healthStatusOK);
  }

}
