package bio.ferlab.clin.qlinme.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

@Slf4j
public class NameUtils {

  public static final Pattern NO_SPECIAL_CHARACTERS = Pattern.compile("^[a-zA-Z0-9- .'\\u00C0-\\u00FF]*$");
  public static final Pattern VALID_RAMQ = Pattern.compile("^[A-Z]{4}\\d{8,9}$");

  public static boolean hasNoSpecialCharacters(String value) {
    return NO_SPECIAL_CHARACTERS.matcher(value).find();
  }

  public static boolean isValidRamq(String value) {
    if (VALID_RAMQ.matcher(value).find()) {
      var dateStr = StringUtils.substring(value, 4, value.length()-2);
      var year = Integer.parseInt(dateStr.substring(0,2));
      var month = Integer.parseInt(dateStr.substring(2,4));
      var day = Integer.parseInt(dateStr.substring(4,6));

      if(month > 50) month -= 50;
      return DateUtils.isValid(String.format("%02d%02d%02d", year, month, day), DateUtils.YYMMDD);
    }
    return false;
  }

}
