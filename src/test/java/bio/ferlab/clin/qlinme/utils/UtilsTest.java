package bio.ferlab.clin.qlinme.utils;

import io.javalin.http.Context;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UtilsTest {

  @Test
  public void testIsPublicRoute() {
    final var context = Mockito.mock(Context.class);
    Mockito.when(context.req()).thenReturn(Mockito.mock(Request.class));
    Mockito.when(context.req().getRequestURI()).thenReturn("/actuator/health");
    assertEquals(true, Utils.isPublicRoute(context));

   }

  @Test
  @DisplayName("Should return correct count of elements in list")
  public void shouldReturnCorrectCountOfElementsInList() {
    List<String> inputList = Arrays.asList("a", "b", "a", "c", "b", "b");
    Map<String, Integer> resultMap = Utils.countBy(inputList);
    assertEquals(2, resultMap.get("a"));
    assertEquals(3, resultMap.get("b"));
    assertEquals(1, resultMap.get("c"));
  }
  @Test
  void testEncodeURLWithSpacesAndSpecialChars() {
    String url = "https://example.com/path with spaces?param=value&special=!@#$%^&*()";
    String expected = "https%3A%2F%2Fexample.com%2Fpath+with+spaces%3Fparam%3Dvalue%26special%3D%21%40%23%24%25%5E%26*%28%29";
    assertEquals(expected, Utils.encodeURL(url));
  }

  @Test
  void testEncodeURLWithNull() {
    assertNull(Utils.encodeURL(null), "encodeURL should return null for null input");
  }

}
