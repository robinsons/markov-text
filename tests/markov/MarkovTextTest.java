package markov;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import markov.MarkovText.Builder;
import org.junit.jupiter.api.Test;

public final class MarkovTextTest {

  // By having one of these be even and the other odd, we ensure that the output text always has an
  // odd length while it is being built. This is leveraged in tests to verify that the final result
  // is correctly truncated to even lengths.
  private static final int PREFIX_LENGTH = 7;
  private static final int SUFFIX_LENGTH = 2;

  private static final Builder ALICE_IN_WONDERLAND_BUILDER =
      MarkovText.fromFile("alice_in_wonderland.txt")
          .withPrefixLength(PREFIX_LENGTH)
          .withSuffixLength(SUFFIX_LENGTH)
          .withSeed(321L);

  private static final String OUTPUT_OF_LENGTH_250 =
      "for when he sneezes; For he can EVEN finished it off. ‘Give your history, Alice had been for"
          + " some more conversation of compliance. To SEND DONATIONS or detach or remove that she "
          + "did not venture to make ONE respectful tone, ‘For the Mouse, in a louder";

  @Test
  public void fromRawTextThrowsIfInputIsNull() {
    assertThrows(NullPointerException.class, () -> MarkovText.fromRawText(null));
  }

  @Test
  public void fromFileThrowsIfInputIsNull() {
    assertThrows(NullPointerException.class, () -> MarkovText.fromFile(null));
  }

  @Test
  public void withPrefixLengthThrowsIfValueIsNonPositive() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ALICE_IN_WONDERLAND_BUILDER.withPrefixLength(0));
    assertThrows(
        IllegalArgumentException.class,
        () -> ALICE_IN_WONDERLAND_BUILDER.withPrefixLength(-17));
  }

  @Test
  public void withSuffixLengthThrowsIfValueIsNonPositive() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ALICE_IN_WONDERLAND_BUILDER.withSuffixLength(0));
    assertThrows(
        IllegalArgumentException.class,
        () -> ALICE_IN_WONDERLAND_BUILDER.withSuffixLength(-101));
  }

  @Test
  public void ofLengthProducesExpectedResult() throws Exception {
    assertThat(ALICE_IN_WONDERLAND_BUILDER.build().ofLength(250)).isEqualTo(OUTPUT_OF_LENGTH_250);
  }

  @Test
  public void ofLengthIsTheCorrectLengthForEvenValues() throws Exception {
    assertThat(ALICE_IN_WONDERLAND_BUILDER.build().ofLength(784).length()).isEqualTo(784);
  }

  @Test
  public void ofLengthIsTheCorrectLengthForOddValues() throws Exception {
    assertThat(ALICE_IN_WONDERLAND_BUILDER.build().ofLength(987).length()).isEqualTo(987);
  }

  @Test
  public void ofLengthIsTheCorrectLengthWhenValueIsLessThanPrefixLength() throws Exception {
    assertThat(ALICE_IN_WONDERLAND_BUILDER.build().ofLength(PREFIX_LENGTH - 1).length())
        .isEqualTo(PREFIX_LENGTH - 1);
  }

  @Test
  public void ofLengthIsTheCorrectLengthWhenValueIsLessThanSuffixLength() throws Exception {
    assertThat(ALICE_IN_WONDERLAND_BUILDER.build().ofLength(SUFFIX_LENGTH - 1).length())
        .isEqualTo(SUFFIX_LENGTH - 1);
  }

  @Test
  public void ofLengthProducesOutputEvenWhenRawTextIsShort() throws Exception {
    // Since the raw text is a totally ordered sequence, the only possible outputs are "abc", "bc",
    // or "c". Thus, ofLength(1000) will never return a result with the desired length.
    assertThat(MarkovText.fromRawText("abc").withSeed(0L).build().ofLength(1000).length())
        .isLessThan(4);
  }
}