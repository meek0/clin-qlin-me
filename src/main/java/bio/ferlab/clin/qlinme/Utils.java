package bio.ferlab.clin.qlinme;

import io.javalin.http.Context;
import io.javalin.validation.NullableValidator;
import org.apache.commons.lang3.StringUtils;

public class Utils {

  public static NullableValidator<String> getValidQueryParam(Context ctx, String name) {
    return ctx.queryParamAsClass(name, String.class).allowNullable().check(StringUtils::isNotBlank, "query param is required");
  }

  public static NullableValidator<String> getValidParamParam(Context ctx, String name) {
    return ctx.pathParamAsClass(name, String.class).allowNullable().check(StringUtils::isNotBlank, "path param is required");
  }

  public static boolean isPublicRoute(Context ctx) {
    return App.config.publics.stream().anyMatch(p -> ctx.req().getRequestURI().startsWith(p));
  }
}
