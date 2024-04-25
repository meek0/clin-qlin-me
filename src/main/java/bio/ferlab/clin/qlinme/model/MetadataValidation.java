package bio.ferlab.clin.qlinme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.*;

@Getter
public class MetadataValidation {

  private final Map<String, List<String>> errors = new LinkedHashMap<>();

  public void addError(String field, String error) {
    errors.computeIfAbsent(field, f -> new ArrayList<>());
    errors.get(field).add(error);
  }

  @JsonIgnore
  public boolean isValid() {
    return errors.isEmpty();
  }
}
