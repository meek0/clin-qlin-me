package bio.ferlab.clin.qlinme.handlers;

import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import io.javalin.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionHandler {

  public void handle(Exception e, Context ctx) {
    if (e instanceof ValidationException ve) {
      ve.getErrors().values().forEach(v -> v.forEach(v1 -> v1.setValue(null)));
      ctx.status(HttpStatus.BAD_REQUEST.getCode()).json(ve.getErrors());
    } else if (e instanceof HttpResponseException re) {
      ctx.status(re.getStatus()).result(re.getMessage());
    } else {
      log.error("", e);
      ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("internal server error");
    }
  }

}
