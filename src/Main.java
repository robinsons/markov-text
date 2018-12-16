import markov.MarkovText;

public final class Main {
  public static void main(String[] args) throws Exception {
    MarkovText prideAndPrejudice =
        MarkovText.fromFile("pride_and_prejudice.txt")
            .withPrefixLength(7)
            .withSuffixLength(4)
            .build();
    System.out.println(prideAndPrejudice.ofLength(1000));
  }
}
