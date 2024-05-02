package bio.ferlab.clin.qlinme.utils;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameUtilsTest {
  @Test
  void isValidRamq() {
    assertTrue(NameUtils.isValidRamq("FOOO01010100"));
    assertFalse(NameUtils.isValidRamq("FOOO01500100"));
    assertTrue(NameUtils.isValidRamq("FOOO01510100"));
    assertFalse(NameUtils.isValidRamq("FOOOxxxxxxxx"));
  }
}
