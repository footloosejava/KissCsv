package github.footloosejava.kisscsv;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


/**
 * An `beep-beep` fast, standards compliant CSV reader.
 */
public class KissReader implements Closeable, Supplier<String[]> {

    private final Reader reader;
    private final KissParser parser;
    private int skipRemainingLines;
    private int recordNumber = 1;

    /**
     * Constructs KissReader using a comma for the separator and a double-quote
     * for the quote character.
     *
     * @param reader the reader to an underlying CSV source.
     */
    public KissReader(Reader reader) {
        this(reader, 0, new KissParser());
    }

    /**
     * Constructs KissReader using a comma for the separator and a double-quote
     * for the quote character.
     *
     * @param reader    the reader to an underlying CSV source.
     * @param skipLines the number of records to skip before reading.
     */
    public KissReader(Reader reader, int skipLines) {
        this(reader, skipLines, new KissParser());
    }

    public KissReader(Reader reader, KissParser csvParser) {
        this(reader, 0, csvParser);
    }

    /**
     * Constructs KissReader with supplied separator and quote char.
     *
     * @param reader    the reader to an underlying CSV source. It is advised to ensure that the source is buffered.
     * @param skipLines the number of lines to skip before reading records.
     * @param csvParser the parser to use to parse input
     */
    public KissReader(Reader reader, int skipLines, KissParser csvParser) {
        this.reader = reader;

        this.skipRemainingLines = skipLines;
        this.parser = csvParser;
    }

    /**
     * Reads the entire file into a List with each element being a String[] of
     * tokens.
     *
     * @return a List of String[], with each String[] representing a line of the
     * file.
     */
    public List<String[]> readAll() {
        List<String[]> records = new ArrayList<>();
        // We assume that each record is going to be the same length as the last
        // record. Initially we set to default.
        // This makes it faster for large parse jobs and prevents unwanted
        // ArrayList expansion in parser.
        int expectedSize = -1;
        String[] nextLineAsTokens;
        while ((nextLineAsTokens = readNext(expectedSize)) != null) {
            expectedSize = nextLineAsTokens.length;
            records.add(nextLineAsTokens);
        }
        return records;
    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     *
     * @return a string array with each comma-separated element as a separate
     * entry.
     */
    public String[] readNext() {
        return readNext(-1);
    }

    @Override
    public String[] get() {
        return readNext();
    }

    /**
     * Reads the next record as a String array.
     *
     * @param expectedSize can be supplied if the record length is known ahead
     *                     of time.
     * @return the record as an array of String.
     */
    public String[] readNext(int expectedSize) {
        try {
            String[] result;
            int skipExpected = expectedSize;
            while (skipRemainingLines > 0 && (result = parser.parseNext(reader, skipExpected)) != null) {
                skipExpected = result.length;
                recordNumber++;
                skipRemainingLines--;
            }

            // if we had a null record above due to EOF then SKIP was exhausted and can be considered zero
            skipRemainingLines = 0;

            result = parser.parseNext(reader, expectedSize);
            recordNumber++;
            return result;
        } catch (Exception e) {
            throw new KissException(e.getMessage() + " (rec no. " + recordNumber + ")", e);
        }
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
