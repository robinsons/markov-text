import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableListMultimap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * This class generates a string whose sequence of characters is probabilistically likely to occur
 * based on a given input text. In other words, the output is a Markov chain based on fixed-length
 * sequences of characters from an input text and the set of possible "suffix" sequences for every
 * such "prefix" sequence.
 *
 * <p>In particular, this class does the following:
 * <ol>
 *   <li>Given an input text, a {@code prefixLength}, and a {@code suffixLength}, compute all
 *       contiguous character subsequences of the input text of length {@code prefixLength} along
 *       with the immediately following contiguous subsequence of length {@code suffixLength}.
 *   <li>Construct a map where every such "prefix subsequence" is a key indexing the set of all
 *       "suffix subsequences" following that prefix.
 *   <li>Select an initial prefix from the set of all prefixes. The likelihood of selecting a given
 *       prefix should correspond to the frequency with which that prefix occurs in the input text
 *       relative to other prefixes.
 *   <li>Construct an output text by iteratively appending a randomly selected suffix of the current
 *       prefix. The next prefix is determined by selecting the last {@code prefixLength} characters
 *       from the output being built.
 *   <li>Once the output text has reached a desired length, it is returned.
 * </ol>
 *
 * <p>Note the input text should be long, otherwise it is likely that this class will fail to
 * produce an output of the desired length.
 *
 * <p>Instances of this object should be constructed using the provided {@link Builder}, for
 * example:
 *
 * <pre>
 *   MarkovText hamlet =
 *       MarkovText.fromFile("hamlet.txt")
 *           .withPrefixLength(7)
 *           .withSuffixLength(4)
 *           .build();
 * </pre>
 */
public final class MarkovText {

  private final ImmutableListMultimap<String, String> markovMap;
  private final Random random;

  private MarkovText(ImmutableListMultimap<String, String> markovMap, Random random) {
    this.markovMap = markovMap;
    this.random = random;
  }

  /**
   * Generates and returns an output text as described by this class's javadoc comment. The output
   * will be of length <= {@code length}.
   */
  public String ofLength(int length) {
    checkArgument(length >= 0, "length must be non-negative.");

    // Begin the output by selecting a random prefix.
    String prefix = randomElement(markovMap.keys().asList()).orElse("");
    StringBuilder builder = new StringBuilder(prefix);

    while (builder.length() < length) {
      // Select a random suffix for the current prefix. If there is none, break and return whatever
      // has been accumulated so far.
      Optional<String> suffix = randomElement(markovMap.get(prefix));
      if (!suffix.isPresent()) {
        break;
      }

      // Append the suffix and update the prefix.
      builder.append(suffix.get());
      prefix = builder.substring(builder.length() - prefix.length());
    }

    // Trim the result to the input length and return it.
    builder.setLength(length);
    return builder.toString();
  }

  /**
   * Returns a random element from the input list, or {@link Optional#empty()} if the list is empty.
   */
  private <T> Optional<T> randomElement(List<T> list) {
    if (list.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(list.get(random.nextInt(list.size())));
  }

  /** Returns a new {@link Builder} with the given {@code rawText} used as the input. */
  public static Builder fromRawText(String rawText) {
    return new Builder(rawText, SourceType.RAW_TEXT);
  }

  /** Returns a new {@link Builder} with the input read from a specified file. */
  public static Builder fromFile(String filename) {
    return new Builder(filename, SourceType.FILE);
  }

  /**
   * Builder class for {@link MarkovText}. Instances should be created using {@link
   * #fromRawText(String)} or {@link #fromFile(String)}.
   */
  public static final class Builder {

    private int prefixLength = 1;
    private int suffixLength = 1;

    private final String rawTextOrFilename;
    private final SourceType sourceType;

    private Builder(String rawTextOrFilename, SourceType sourceType) {
      this.rawTextOrFilename = checkNotNull(rawTextOrFilename);
      this.sourceType = checkNotNull(sourceType);
    }

    /**
     * Sets the {@link #prefixLength} for this builder. Must be > 0. A larger value will result in
     * output text that more closely resembles the input.
     */
    public Builder withPrefixLength(int prefixLength) {
      checkArgument(prefixLength > 0);
      this.prefixLength = prefixLength;
      return this;
    }

    /** Sets the {@link #suffixLength} for this builder. Must be > 0. */
    public Builder withSuffixLength(int suffixLength) {
      checkArgument(suffixLength > 0);
      this.suffixLength = suffixLength;
      return this;
    }

    /** Returns a new {@link MarkovText} from this builder's parameters. */
    public MarkovText build() throws IOException {
      // Fetch the rawText and then sanitize it by removing extraneous whitespace. By sanitizing the
      // text we achieve nicer looking outputs.
      String rawText = sourceType.toRawText(rawTextOrFilename);
      String sanitizedText = rawText.replaceAll("\\s+", " ");
      return new MarkovText(buildMarkovMap(sanitizedText), new Random());
    }

    /**
     * Returns a map whose keys are the prefixes and whose values are lists of suffixes following
     * those prefixes. See the javadoc for this class for a description of the terms "prefix" and
     * "suffix".
     */
    private ImmutableListMultimap<String, String> buildMarkovMap(String text) {
      ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();

      for (int i = 0; i <= text.length() - prefixLength - suffixLength; i++) {
        String prefix = text.substring(i, i + prefixLength);
        String suffix = text.substring(i + prefixLength, i + prefixLength + suffixLength);
        builder.put(prefix, suffix);
      }

      return builder.build();
    }
  }

  /**
   * Helper for describing what type of input was provided to a {@link Builder}. This is used as a
   * delegate to abstract away the difference between raw text and filenames during {@link
   * Builder#build()}.
   */
  private enum SourceType {
    RAW_TEXT {
      @Override
      String toRawText(String input) {
        return input;
      }
    },

    FILE {
      @Override
      String toRawText(String input) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"), "res", input);
        return Files.lines(path).parallel().reduce("", (a, b) -> a + " " + b);
      }
    };

    /**
     * Given a string that was passed as input to {@link Builder#Builder(String, SourceType)}, fetch
     * the raw text referenced by that input.
     */
    abstract String toRawText(String input) throws IOException;
  }
}
