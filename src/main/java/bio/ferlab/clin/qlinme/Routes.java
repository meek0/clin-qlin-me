package bio.ferlab.clin.qlinme;

public class Routes {

  public static final String ACTUATOR_HEALTH = "/actuator/health";
  public static final String BASE_API_V1 = "/api/v1";
  public static final String AUTH_LOGIN = BASE_API_V1 + "/auth/login";
  public static final String BATCH = BASE_API_V1 + "/batch/{batch_id}";
  public static final String BATCH_HISTORY = BASE_API_V1 + "/batch/{batch_id}/history";
  public static final String BATCH_HISTORY_BY_VERSION = BASE_API_V1 + "/batch/{batch_id}/history/{version}";
  public static final String BATCH_STATUS = BASE_API_V1 + "/batch/{batch_id}/status";
}
