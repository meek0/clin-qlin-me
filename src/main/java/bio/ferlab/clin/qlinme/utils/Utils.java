package bio.ferlab.clin.qlinme.utils;

import bio.ferlab.clin.qlinme.App;
import io.javalin.http.Context;
import io.javalin.validation.NullableValidator;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

  public static NullableValidator<String> getValidQueryParam(Context ctx, String name) {
    return ctx.queryParamAsClass(name, String.class).allowNullable().check(StringUtils::isNotBlank, "query param is required");
  }

  public static NullableValidator<String> getValidParamParam(Context ctx, String name) {
    return ctx.pathParamAsClass(name, String.class).allowNullable().check(StringUtils::isNotBlank, "path param is required");
  }

  public static boolean isPublicRoute(Context ctx) {
    return App.CONFIG.publics.stream().anyMatch(p -> ctx.req().getRequestURI().startsWith(p));
  }

  public static <T> Map<T, Integer> countBy(List<T> inputList) {
    Map<T, Integer> resultMap = new HashMap<>();
    inputList.forEach(e -> resultMap.merge(e, 1, Integer::sum));
    return resultMap;
  }

  public static String encodeURL(String url) {
    return URLEncoder.encode(url, StandardCharsets.UTF_8);
  }
}
