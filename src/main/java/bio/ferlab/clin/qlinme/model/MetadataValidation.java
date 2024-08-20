package bio.ferlab.clin.qlinme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.javalin.openapi.OpenApiIgnore;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MetadataValidation {

  @Setter
  private String schema;
  @Setter
  private String batchId;
  @Setter
  private int analysesCount = 0;
  private final Map<String, List<String>> errors = new LinkedHashMap<>();
  private final Map<String, List<String>> warnings = new LinkedHashMap<>();

  public void addError(String field, String msg) {
    errors.computeIfAbsent(field, f -> new ArrayList<>());
    errors.get(field).add(msg);
  }

  public void addWarning(String field, String msg) {
    warnings.computeIfAbsent(field, f -> new ArrayList<>());
    warnings.get(field).add(msg);
  }

  @JsonIgnore
  @OpenApiIgnore
  public boolean isValid() {
    return errors.isEmpty() & analysesCount > 0;
  }

  public String getSchema() {
    return schema;
  }

  public String getBatchId() {
    return batchId;
  }

  public int getAnalysesCount() {
    return analysesCount;
  }

  public Map<String, List<String>> getErrors() {
    return errors;
  }

  public Map<String, List<String>> getWarnings() {
    return warnings;
  }

}
