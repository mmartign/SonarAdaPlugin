package com.spazioit.sonarada.adacontrol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdaControlArgumentParserTest {

  private final AdaControlArgumentParser parser = new AdaControlArgumentParser();

  @Test
  void parsesQuotedArguments() {
    assertThat(parser.parse("-- -I\"src generated\" -gnat12"))
      .containsExactly("--", "-Isrc generated", "-gnat12");
  }

  @Test
  void returnsEmptyListForBlankArguments() {
    assertThat(parser.parse("  ")).isEmpty();
  }
}
