package bio.ferlab.clin.qlinme.utils;

import java.text.SimpleDateFormat;
import java.util.Arrays;

public class DateUtils {

  public static final String DDMMYYYY = "dd/MM/yyyy";
  public static final String YYYYMMDD = "yyyy-MM-dd";
  public static final String YYMMDD = "yyMMdd";

  public static boolean isValid(String date, String...formats) {
    return Arrays.stream(formats).anyMatch(f -> isValid(date, f));
  }

  public static boolean isValid(String date, String format) {
    try {
      // SimpleDateFormat isnt thread-safe, create a new one
      var df = new SimpleDateFormat(format);
      var parsed = df.parse(date);
      // parse + format will check both format en out of range number of days/month
      return df.format(parsed).equals(date);
    } catch(Exception e) {
      return false;
    }
  }
}
