package github.footloosejava.kisscsv;

import java.io.*;

public final class KissWriter implements Closeable, Flushable {

    public static final char DEFAULT_SEPARATOR = ',';
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    public enum LINE_END {

        DEFAULT, LF, CRLF;

        @Override
        public String toString() {
            if (this == CRLF) {
                return "\r\n";
            } else {
                return "\n";
            }
        }
    }

    private final BufferedWriter writer;
    private final char separator;
    private final char quotechar;
    private final String lineEnd;

    /**
     * Constructs CsvWriter using a comma for the separator.
     *
     * @param writer the writer to an underlying CSV source.
     */
    public KissWriter(Writer writer) {
        this(DEFAULT_SEPARATOR, writer);
    }

    /**
     * @param writer    the writer to an underlying CSV source.
     * @param separator the delimiter to use for separating entries.
     */
    public KissWriter(char separator, Writer writer) {
        this(separator, writer, DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * @param writer    the writer to write to
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     */
    public KissWriter(char separator, Writer writer, char quotechar) {
        this(separator, writer, quotechar, LINE_END.LF);
    }

    /**
     * @param writer    the writer to write to
     * @param separator the delimiter to use
     * @param quotechar the character to use for quoted fields
     * @param lineEnd   the line ending to use for records
     */
    public KissWriter(char separator, Writer writer, char quotechar, LINE_END lineEnd) {
        this.writer = writer instanceof BufferedWriter
            ? (BufferedWriter) writer
            : new BufferedWriter(writer);

        this.separator = separator;
        this.quotechar = quotechar;
        this.lineEnd = lineEnd.toString();
    }

    /**
     * Writes all the records (String[]) that can be obtained from the iterable.
     *
     * @param iterable         an Iterable<String[]>, with each String[] representing a
     *                         record of 0 or more fields.
     * @param applyQuotesToAll true if all values are to be quoted. false if
     *                         quotes only to be applied to values which contain the separator, escape,
     *                         quote or new line characters.
     */
    public void writeAll(Iterable<String[]> iterable, boolean applyQuotesToAll) {
        for (String[] line : iterable) {
            writeNext(applyQuotesToAll, line);
        }
    }

    /**
     * Writes all the records (String[]) that can be obtained from the iterable.
     *
     * @param iterable an Iterable<String[]>, with each String[] representing a
     *                 record of 0 or more fields. Fields will not be quoted unless necessary.
     */
    public void writeAll(Iterable<String[]> iterable) {
        writeAll(iterable, false);
    }

    /**
     * Writes the next record (String[])
     *
     * @param nextLine a string array with each comma-separated element as a
     *                 separate entry.
     * @param quoteAll Force all fields in a record to be surrounded in quotes.
     *                 Otherwise, quotes will only be used in fields when necessary.
     */
    public void writeNext(boolean quoteAll, String... nextLine) {
        try {
            if (nextLine == null) {
                return;
            }

            for (int i = 0; i < nextLine.length; i++) {
                if (i != 0) {
                    writer.write(separator);
                }

                String nextElement = nextLine[i];
                // nulls and empties just become empty fields
                if (nextElement == null || nextElement.isEmpty()) {
                    continue;
                }

                if (quoteAll) {
                    writeQuotedField(nextElement);
                } else {
                    if (checkNeedsQuotes(nextElement)) {
                        writeQuotedField(nextElement);
                    } else {
                        writer.write(nextElement);
                    }
                }
            }

            writer.append(lineEnd);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private boolean checkNeedsQuotes(String nextElement) {
        for (int n = 0; n < nextElement.length(); n++) {
            final char c = nextElement.charAt(n);
            if (c == quotechar || c == separator
                || c == '\n' || c == '\r') {
                return true;
            }
        }
        return false;
    }

    private void writeQuotedField(String nextElement) {
        try {
            // simple: escape all quotes only
            writer.append(quotechar);
            for (int n = 0; n < nextElement.length(); n++) {
                char c = nextElement.charAt(n);
                if (c == quotechar) {
                    // all quote chars within quote chars must be doubled
                    writer.write(quotechar);
                }
                writer.write(c);
            }
            writer.append(quotechar);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine a string array with each comma-separated element as a
     *                 separate entry.
     */
    public void writeNext(String... nextLine) {
        writeNext(false, nextLine);
    }

    protected void processLine(final Writer w, final String nextElement) {
        try {
            for (int j = 0; j < nextElement.length(); j++) {
                char nextChar = nextElement.charAt(j);
                if (nextChar == quotechar) {
                    w.append(quotechar).append(quotechar);
                } else {
                    w.append(nextChar);
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
