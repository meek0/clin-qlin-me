package bio.ferlab.clin.qlinme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FilesValidation {

  @Setter
  private int count = 0;

  private final List<String> errors = new ArrayList<>();

  public void addError(String error) {
    errors.add(error);
  }

  @JsonIgnore
  public boolean isValid() {
    return errors.isEmpty() & count > 0;
  }
}
