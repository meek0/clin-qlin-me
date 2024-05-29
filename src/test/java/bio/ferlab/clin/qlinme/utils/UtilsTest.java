package bio.ferlab.clin.qlinme.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {

  @Test
  @DisplayName("Should return correct count of elements in list")
  public void shouldReturnCorrectCountOfElementsInList() {
    List<String> inputList = Arrays.asList("a", "b", "a", "c", "b", "b");
    Map<String, Integer> resultMap = Utils.countBy(inputList);
    assertEquals(2, resultMap.get("a"));
    assertEquals(3, resultMap.get("b"));
    assertEquals(1, resultMap.get("c"));
  }
}
