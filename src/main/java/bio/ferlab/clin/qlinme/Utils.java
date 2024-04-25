package bio.ferlab.clin.qlinme;

import io.javalin.http.Context;
import io.javalin.validation.NullableValidator;
import org.apache.commons.lang3.StringUtils;

public class Utils {

  public static NullableValidator<String> getValidQueryParam(Context ctx, String name) {
    return ctx.queryParamAsClass(name, String.class).allowNullable().check(StringUtils::isNotBlank, "query param is required");
  }
}
