package bio.ferlab.clin.qlinme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.javalin.openapi.OpenApiIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
  @OpenApiIgnore
  public boolean isValid() {
    return errors.isEmpty() & analysesCount > 0;
  }

  public String getSchema() {
    return schema;
  }

  public int getAnalysesCount() {
    return analysesCount;
  }

  public Map<String, List<String>> getErrors() {
    return errors;
  }

}
