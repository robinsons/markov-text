import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableListMultimap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public final class MarkovText {

  private final ImmutableListMultimap<String, String> markovMap;
  private final Random random;

  private MarkovText(ImmutableListMultimap<String, String> markovMap, Random random) {
    this.markovMap = markovMap;
    this.random = random;
  }

  public String ofLength(int length) {
    checkArgument(length >= 0);
    String prefix = randomElement(markovMap.keys().asList());
    StringBuilder builder = new StringBuilder(prefix);
    while (builder.length() < length) {
      String suffix = randomElement(markovMap.get(prefix));
      builder.append(suffix);

      String combined = prefix + suffix;
      prefix = combined.substring(combined.length() - prefix.length());
    }
    return builder.toString();
  }

  private <T> T randomElement(List<T> list) {
    return list.get(random.nextInt(list.size()));
  }

  public static Builder fromRawText(String rawText) {
    return new Builder(rawText, SourceType.RAW_TEXT);
  }

  public static Builder fromFile(String filename) {
    return new Builder(filename, SourceType.FILE);
  }

  public static final class Builder {

    private int prefixLength = 1;
    private int suffixLength = 1;

    private final String rawTextOrFilename;
    private final SourceType sourceType;

    private Builder(String rawTextOrFilename, SourceType sourceType) {
      this.rawTextOrFilename = checkNotNull(rawTextOrFilename);
      this.sourceType = checkNotNull(sourceType);
    }

    public Builder withPrefixLength(int prefixLength) {
      checkArgument(prefixLength > 0);
      this.prefixLength = prefixLength;
      return this;
    }

    public Builder withSuffixLength(int suffixLength) {
      checkArgument(suffixLength > 0);
      this.suffixLength = suffixLength;
      return this;
    }

    public MarkovText build() throws IOException {
      String rawText = sourceType.toRawText(rawTextOrFilename);
      String sanitizedText = rawText.replaceAll("\\s+", " ");
      return new MarkovText(buildMarkovMap(sanitizedText), new Random());
    }

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

  private enum SourceType {
    RAW_TEXT {
      @Override
      String toRawText(String input) throws IOException {
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

    abstract String toRawText(String input) throws IOException;
  }
}
