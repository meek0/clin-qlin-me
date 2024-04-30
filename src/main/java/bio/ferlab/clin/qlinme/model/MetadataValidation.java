package bio.ferlab.clin.qlinme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class MetadataValidation {
  @Setter
  private String schema;
  @Setter
  private int analysesCount = 0;
  private final Map<String, List<String>> errors = new LinkedHashMap<>();

  public void addError(String field, String error) {
    errors.computeIfAbsent(field, f -> new ArrayList<>());
    errors.get(field).add(error);
  }

  @JsonIgnore
  public boolean isValid() {
    return errors.isEmpty() & analysesCount > 0;
  }
}
