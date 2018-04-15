package github.footloosejava.kisscsv;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class KissParser {

    private static final boolean debug = Boolean.getBoolean(KissParser.class.getName() + "-debug");

    public static final char DEFAULT_SEPARATOR = ',';
    public static final char DEFAULT_QUOTE_CHAR = '"';
    public static final boolean DEFAULT_TRIM_RESULTS = false;
    public static final String[] EMPTY_ARRAY = new String[0];

    private final char separator;
    private final boolean hasquotechar;
    private final char quotechar;
    private final boolean trimResults;

    public KissParser() {
        separator = DEFAULT_SEPARATOR;
        hasquotechar = true;
        quotechar = DEFAULT_QUOTE_CHAR;
        trimResults = DEFAULT_TRIM_RESULTS;
    }

    /**
     * @param separator Separator to use
     */
    public KissParser(char separator) {
        this(separator, DEFAULT_QUOTE_CHAR, DEFAULT_TRIM_RESULTS);
    }

    /**
     * @param trimResults All surrounding whitespace removed from final value of
     *                    fields.
     */
    public KissParser(boolean trimResults) {
        this(DEFAULT_SEPARATOR, trimResults);
    }

    /**
     * @param separator              The separator character to use.
     * @param optionalQuoteCharacter Optional quote character. Can be set to empty
     *                               to empty to have no quote character.
     */
    public KissParser(char separator, Character optionalQuoteCharacter) {
        this(separator, optionalQuoteCharacter, DEFAULT_TRIM_RESULTS);
    }

    /**
     * @param separator   Separator to use
     * @param trimResults Trims the final value of all fields in a record.
     */
    public KissParser(char separator, boolean trimResults) {
        this(separator, DEFAULT_QUOTE_CHAR, trimResults);
    }

    /**
     * @param separator              Separator to use
     * @param optionalQuoteCharacter Optional quote character. Can be set to null to have no quote character.
     * @param trimResults            Trims the final value of all fields in a record.
     */
    public KissParser(char separator, Character optionalQuoteCharacter, boolean trimResults) {
        this.separator = separator;
        this.hasquotechar = optionalQuoteCharacter != null;
        this.quotechar = this.hasquotechar ? optionalQuoteCharacter : '\0';
        this.trimResults = trimResults;
        if (this.hasquotechar && this.separator == this.quotechar) {
            throw new IllegalArgumentException("Separator and Quote characters cannot be the same!");
        }
    }

    private boolean isQuoteChar(int n) {
        return hasquotechar && n == quotechar;
    }

    /**
     * Helper method to trim the StringBuilder. We use the same whitespace
     * determination as String.trim()
     */
    private String result(StringBuilder sb, boolean trim) {
        if (trim) {
            int len = sb.length();
            int start = 0;
            while ((start < len) && (sb.charAt(start) <= ' ')) {
                start++;
            }
            while ((start < len) && (sb.charAt(len - 1) <= ' ')) {
                len--;
            }
            return ((start > 0) || (len < sb.length()) ? sb.substring(start, len) : sb.toString());
        } else {
            return sb.toString();
        }
    }

    /**
     * Parses the next record (set of fields).
     *
     * @param reader Reader to read from.
     * @return parsed tokens as String[]
     */
    public String[] parseNext(final Reader reader) {
        return parseNext(reader, -1);
    }

    /**
     * Parses the next record (set of fields)
     *
     * @param reader             Reader to read from.
     * @param expectedRecordSize If the expected size is known, use it here.
     * @return parsed tokens as String[]
     */
    public String[] parseNext(final Reader reader, final int expectedRecordSize) {
        List<String> fields = new ArrayList<>(expectedRecordSize >= 0 ? expectedRecordSize : 10);
        int count = parseNext(reader, fields::add);
        return count == -1 ? null : fields.toArray(EMPTY_ARRAY);
    }

    /**
     * Parses the next record (set of fields). This method makes it possible to avoid a list creation and String[] creationg with each record.
     *
     * @param reader   Reader to read from.
     * @param consumer The consumer for each field in the record.
     * @return the number of fields in the record or -1 if EOF
     */
    public int parseNext(final Reader reader, final Consumer<String> consumer) {
        try {
            int r = reader.read();
            if (r == -1) {
                return -1;
            }
            final StringBuilder working = new StringBuilder();
            int count = 0;
            boolean inQuotes = false;
            boolean endOfField = false;

            while (r != -1) {
                if (debug) {
                    System.out.println(r + " = '" + (char) r + "'");
                }
                // there are only two states:
                // * in quotes
                // * not in quotes
                if (inQuotes) {
                    if (isQuoteChar(r)) {
                        if (isQuoteChar(r = reader.read())) {
                            // doubled quotes: just append a quote and carry on
                            working.append((char) r);
                        } else {
                            // end quote is end of field
                            endOfField = true;
                            inQuotes = false;
                            continue;
                        }
                    } else {
                        working.append((char) r);
                    }
                } else {
                    if (isQuoteChar(r)) {
                        if (endOfField) {
                            throw new KissException("Record has already been closed (matched quotes found)");
                        }
                        // if we encounter a first quote and it is whitespace until that point
                        // we ignore the whitespace as outside the quote
                        working.setLength(0);
                        inQuotes = true;
                    } else if (r == separator) {
                        // add to fields
                        count++;
                        consumer.accept(result(working, trimResults));

                        // RESET
                        working.setLength(0);
                        inQuotes = false;
                        endOfField = false;

                    } else if (r == '\n') {
                        // END OF RECORD
                        break;
                    } else if (r == '\r') {
                        if ((r = reader.read()) == '\n') {
                            // CR + LF is also END OF RECORD
                            break;
                        } else {
                            // OTHERWISE IT IS A CR IN THE FIELD
                            // BUT IF ENDOFFIELD, IT IS JUST WHITESPACE TO BE IGNORED
                            if (endOfField) {
                                continue;
                            } else {
                                // but a CR not followed by a LF is in fact a character to keep
                                working.append('\r');
                                continue;
                            }
                        }
                    } else if (endOfField) {
                        if (r > ' ') {
                            // IF WHITESPACE, WE IGNORE UNTIL SEPARATOR IS FOUND.
                            // OTHERWISE NO TEXT SHOULD COME AFTER END OF FIELD
                            throw new KissException("Non-whitespace character found after last quote in quoted value" + "\n" +
                                "> fields found= " + count + "\n" +
                                "> working field= " + working + "\n" +
                                "> character found= " + ((char) r) + "\n" +
                                "> separator= " + separator + "\n" +
                                "> separator name= " + Character.getName(separator));
                        }
                    } else {
                        working.append((char) r);
                    }
                }
                // we take next char at end because
                // we sometimes continue to decide after already
                // fetching next character
                r = reader.read();
            }
            if (inQuotes) {
                throw new KissException("Un-terminated quoted field at end of CSV record");
            }
            count++;
            consumer.accept(result(working, trimResults));
            return count;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}