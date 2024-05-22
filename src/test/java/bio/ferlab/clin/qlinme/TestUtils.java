package bio.ferlab.clin.qlinme;

import bio.ferlab.clin.qlinme.model.FilesValidation;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.model.MetadataValidation;
import bio.ferlab.clin.qlinme.model.VCFsValidation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.validation.Validation;
import org.apache.commons.io.IOUtils;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TestUtils {

  public static final ObjectMapper mapper = new ObjectMapper()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static Metadata loadTestMetadata(String name) {
    try {
      var content = IOUtils.resourceToString("metadatas/" +name+".json", StandardCharsets.UTF_8, TestUtils.class.getClassLoader());
       return mapper.readValue(content, Metadata.class);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T loadValidation(String name, Class<T> c) {
    try {
      var content = IOUtils.resourceToString("validations/" +name+".json", StandardCharsets.UTF_8, TestUtils.class.getClassLoader());
      return mapper.readValue(content, c);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void assertValidation(String name, MetadataValidation validation) {
    try {
    var expected = loadValidation(name, MetadataValidation.class);
    assertEquals(sanitizeToJSON(expected), sanitizeToJSON(validation));
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void assertValidation(String name, VCFsValidation validation) {
    try {
      var expected = loadValidation(name, VCFsValidation.class);
      assertEquals(sanitizeToJSON(expected), sanitizeToJSON(validation));
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void assertValidation(String name, FilesValidation validation) {
    try {
      var expected = loadValidation(name, FilesValidation.class);
      assertEquals(sanitizeToJSON(expected), sanitizeToJSON(validation));
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String sanitizeToJSON(Object content) throws JsonProcessingException {
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(content);
  }
}
