package bio.ferlab.clin.qlinme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class FilesValidation {

  @Setter
  private int filesCount = 0;
  @Setter
  private int vcfsCount = 0;
  private final List<String> errors = new ArrayList<>();

  public void addError(String error) {
    errors.add(error);
  }

  @JsonIgnore
  public boolean isValid() {
    return errors.isEmpty() & vcfsCount > 0 & filesCount > 0;
  }
}
