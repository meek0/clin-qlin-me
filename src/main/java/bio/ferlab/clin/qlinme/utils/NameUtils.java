package bio.ferlab.clin.qlinme.utils;

import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class NameUtils {

  public static final Pattern NO_SPECIAL_CHARACTERS = Pattern.compile("^[a-zA-Z0-9- .'\\u00C0-\\u00FF]*$");
  public static final Pattern VALID_RAMQ = Pattern.compile("^[A-Z]{4}\\d{8,9}$");

  public static boolean hasNoSpecialCharacters(String value) {
    return NO_SPECIAL_CHARACTERS.matcher(value).find();
  }

  public static boolean isValidRamq(String value) {
    return VALID_RAMQ.matcher(value).find() && DateUtils.isValid(StringUtils.substring(value, 4, value.length()-2), DateUtils.YYMMDD);
  }

}
