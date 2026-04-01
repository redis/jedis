package redis.server.stub.util;

/**
 * Binary-safe glob pattern matcher for Redis PSUBSCRIBE patterns.
 * 
 * <p>Supports standard glob patterns:
 * <ul>
 *   <li>{@code *} - matches zero or more characters</li>
 *   <li>{@code ?} - matches exactly one character</li>
 *   <li>{@code [abc]} - matches one character from the set</li>
 *   <li>{@code [a-z]} - matches one character from the range</li>
 *   <li>{@code [^abc]} or {@code [!abc]} - matches one character NOT in the set</li>
 * </ul>
 * 
 * <p>This implementation is binary-safe and works on byte arrays, not strings.
 * This matches Redis behavior where channel names can contain any byte sequence.
 * 
 * <p>Examples:
 * <pre>
 * matches("news.*", "news.sports")      -> true
 * matches("news.*", "news.weather")     -> true
 * matches("*.sports", "news.sports")    -> true
 * matches("new?", "news")               -> true
 * matches("[abc]*", "apple")            -> true
 * </pre>
 */
public class GlobPatternMatcher {

  /**
   * Check if channel matches glob pattern (binary-safe).
   * 
   * @param pattern the glob pattern (e.g., "news.*")
   * @param channel the channel name to match
   * @return true if channel matches pattern
   */
  public static boolean matches(byte[] pattern, byte[] channel) {
    return matchesInternal(pattern, 0, channel, 0);
  }

  /**
   * Internal recursive matching logic.
   * 
   * @param pattern the pattern bytes
   * @param pIndex current position in pattern
   * @param text the text bytes
   * @param tIndex current position in text
   * @return true if remaining pattern matches remaining text
   */
  private static boolean matchesInternal(byte[] pattern, int pIndex, byte[] text, int tIndex) {
    int pLen = pattern.length;
    int tLen = text.length;

    // Both exhausted -> match
    if (pIndex == pLen && tIndex == tLen) {
      return true;
    }

    // Pattern exhausted, text remaining -> no match
    if (pIndex == pLen) {
      return false;
    }

    // Get current pattern character
    byte p = pattern[pIndex];

    // Handle special characters
    if (p == '*') {
      // Try matching zero or more characters
      // Option 1: Skip '*' and match zero characters
      if (matchesInternal(pattern, pIndex + 1, text, tIndex)) {
        return true;
      }

      // Option 2: Match one character and keep '*'
      if (tIndex < tLen && matchesInternal(pattern, pIndex, text, tIndex + 1)) {
        return true;
      }

      return false;

    } else if (p == '?') {
      // Match exactly one character (any character)
      if (tIndex == tLen) {
        return false; // No character to match
      }
      return matchesInternal(pattern, pIndex + 1, text, tIndex + 1);

    } else if (p == '[') {
      // Character class: [abc], [a-z], [^abc], [!abc]
      if (tIndex == tLen) {
        return false; // No character to match
      }

      // Find closing ]
      int closeIndex = findClosingBracket(pattern, pIndex + 1);
      if (closeIndex == -1) {
        // No closing bracket -> treat '[' as literal
        if (tIndex < tLen && text[tIndex] == p) {
          return matchesInternal(pattern, pIndex + 1, text, tIndex + 1);
        }
        return false;
      }

      // Extract character class content
      boolean negate = false;
      int classStart = pIndex + 1;

      if (classStart < closeIndex && (pattern[classStart] == '^' || pattern[classStart] == '!')) {
        negate = true;
        classStart++;
      }

      boolean matched = matchesCharClass(pattern, classStart, closeIndex, text[tIndex]);

      if (negate) {
        matched = !matched;
      }

      if (matched) {
        return matchesInternal(pattern, closeIndex + 1, text, tIndex + 1);
      }
      return false;

    } else {
      // Literal character
      if (tIndex == tLen || text[tIndex] != p) {
        return false;
      }
      return matchesInternal(pattern, pIndex + 1, text, tIndex + 1);
    }
  }

  /**
   * Find the closing bracket for a character class.
   * 
   * @param pattern the pattern
   * @param start start index (after '[')
   * @return index of ']', or -1 if not found
   */
  private static int findClosingBracket(byte[] pattern, int start) {
    for (int i = start; i < pattern.length; i++) {
      if (pattern[i] == ']') {
        return i;
      }
    }
    return -1;
  }

  /**
   * Check if character matches a character class like [abc] or [a-z].
   * 
   * @param pattern the pattern
   * @param start start of class content (after '[' and optional '^')
   * @param end end of class content (before ']')
   * @param ch the character to match
   * @return true if character is in the class
   */
  private static boolean matchesCharClass(byte[] pattern, int start, int end, byte ch) {
    for (int i = start; i < end; i++) {
      // Check for range: a-z
      if (i + 2 < end && pattern[i + 1] == '-') {
        byte rangeStart = pattern[i];
        byte rangeEnd = pattern[i + 2];
        if (ch >= rangeStart && ch <= rangeEnd) {
          return true;
        }
        i += 2; // Skip range
      } else {
        // Literal character
        if (pattern[i] == ch) {
          return true;
        }
      }
    }
    return false;
  }
}

