package bio.ferlab.clin.qlinme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class VCFsValidation {

  @Setter
  private int count = 0;
  @Setter
  private final List<String> errors = new ArrayList<>();
  @Setter
  private final List<String> warnings = new ArrayList<>();

  public void addError(String error) {
    errors.add(error);
  }

  public void addWarning(String warning) {
    warnings.add(warning);
  }

  @JsonIgnore
  public boolean isValid() {
    return errors.isEmpty() & count > 0;
  }
}
