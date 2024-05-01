package bio.ferlab.clin.qlinme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.javalin.openapi.OpenApiIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VCFsValidation {

  @Setter
  private int count = 0;
  private final List<String> errors = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();

  public void addError(String error) {
    errors.add(error);
  }

  public void addWarning(String warning) {
    warnings.add(warning);
  }

  @JsonIgnore
  @OpenApiIgnore
  public boolean isValid() {
    return errors.isEmpty() & count > 0;
  }

  public int getCount() {
    return count;
  }

  public List<String> getErrors() {
    return errors;
  }

  public List<String> getWarnings() {
    return warnings;
  }
}
