package bio.ferlab.clin.qlinme;

public class Routes {

  public static final String ACTUATOR_HEALTH = "/actuator/health";
  public static final String BASE_API_V1 = "/api/v1";
  public static final String AUTH_LOGIN = BASE_API_V1 + "/auth/login";
  public static final String BATCH_POST = BASE_API_V1 + "/batch/{batch_id}";
}
