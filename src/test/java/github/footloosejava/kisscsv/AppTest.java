package github.footloosejava.kisscsv;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertArrayEquals;

public class AppTest extends TestCase {

    public AppTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    public void test1() {
        String data = "NAME \t FAXEMAIL\tBOB   \r\n";
        // so we know what it writes as default now.
        KissParser cp = new KissParser('\t', '"', false);
        KissReader cr = new KissReader(new StringReader(data), cp);

        // firstline
        String[] expected = {"NAME ", " FAXEMAIL", "BOB   "};
        String[] found = cr.readNext();
        assertArrayEquals(expected, found);

        // no more
        assertArrayEquals(null, cr.readNext());
    }

    public void test1b() {
        String data = "NAME \t FAXEMAIL\tBOB   \r\n";
        // so we know what it writes as default now.
        KissParser cp = new KissParser('\t', '"', false);
        KissReader cr = new KissReader(new StringReader(data), cp);

        // firstline
        String[] expected = {"NAME ", " FAXEMAIL", "BOB   "};
        String[] found = cr.readNext(3);
        assertArrayEquals(expected, found);

        // no more
        assertArrayEquals(null, cr.readNext(3));
    }

    public void testMake() {

        StringWriter sw = new StringWriter();
        try (KissWriter cw = new KissWriter(sw)) {
            cw.writeNext("C1", "C2", "C3");
            cw.writeNext("V1", "V2", "V3");
        }
        String data = "C1,C2,C3\n"
            + "V1,V2,V3\n";

        assertEquals(data, sw.toString());

        // so we know what it writes as default now.
        KissReader cr = new KissReader(new StringReader(data));

        // firstline
        String[] expected = {"C1", "C2", "C3"};
        String[] found = cr.readNext();
        assertArrayEquals(expected, found);

        // second line
        expected = new String[]{"V1", "V2", "V3"};
        found = cr.readNext();
        assertArrayEquals(expected, found);

        // no more
        assertArrayEquals(null, cr.readNext());
    }

    public void testMake2() {

        String data = "C1,C2,C3\r\n"
            + "V1,V2,V3\r\n";

        KissReader cr = new KissReader(new StringReader(data));

        // firstline
        String[] expected = {"C1", "C2", "C3"};
        String[] found = cr.readNext();
        assertArrayEquals(expected, found);

        // second line
        expected = new String[]{"V1", "V2", "V3"};
        found = cr.readNext();
        assertArrayEquals(expected, found);

        // no more
        assertArrayEquals(null, cr.readNext());
    }

    public void testMake3a() {

        String data = " \"B   \"   \r\n";

        KissReader cr = new KissReader(new StringReader(data));

        // firstline
        String[] expected = {"B   "};
        String[] found = cr.readNext();
        assertArrayEquals(expected, found);
        assertArrayEquals(null, cr.readNext());
    }

    public void testMake3b() {

        String data = "A, \"B   \"   \r\n"
            + "V1,V2, V3 \r\n";

        KissReader cr = new KissReader(new StringReader(data));

        // firstline
        String[] expected = {"A", "B   "};
        String[] found = cr.readNext();
        assertArrayEquals(expected, found);

        // second line
        expected = new String[]{"V1", "V2", " V3 "};
        found = cr.readNext();
        assertArrayEquals(expected, found);

        // no more
        assertArrayEquals(null, cr.readNext());
    }

    public void testMake3bTrim() {

        String data = "A, \"B   \"   \r\n"
            + "V1,V2,  V3   \r\n";

        KissParser kp = new KissParser(true);
        KissReader kr = new KissReader(new StringReader(data), kp);

        // firstline
        String[] expected = {"A", "B"};
        String[] found = kr.readNext();
        assertArrayEquals(expected, found);

        // second line
        expected = new String[]{"V1", "V2", "V3"};
        found = kr.readNext();
        assertArrayEquals(expected, found);

        // no more
        assertArrayEquals(null, kr.readNext());
    }

    public void testMultiLineQuoted() {
        String data = "C1,C2,\"\"\"C3\r\n"
            + ",V1,V2,V3\"\r\n";

        KissReader cr = new KissReader(new StringReader(data));

        // firstline
        String[] expected = {"C1", "C2", "\"C3\r\n,V1,V2,V3"};
        String[] found = cr.readNext();
        assertArrayEquals(expected, found);
        assertArrayEquals(null, cr.readNext());
    }

    public void testUnterminatedQuotedField() {
        String data = "C1,C2, \" \"\"C3\r\n"
            + "V1,V2,V3\r\n";

        KissReader cr = new KissReader(new StringReader(data));

        try {
            cr.readNext();
            fail("Should throw IOException about unterminate quote at end of field.");
        } catch (Exception ex) {
            assertEquals(
                new KissException("Un-terminated quoted field at end of CSV record (rec no. 1)").toString(), ex.toString());
        }
    }

    public void testEmpty() {
        KissReader cr = new KissReader(new StringReader(""));
        assertNull(cr.readNext());
        assertArrayEquals(null, cr.readNext());
    }

    public void testBlankLine() {
        String data = "\n";

        KissReader cr = new KissReader(new StringReader(data));

        String[] expected = {""};
        String[] found = cr.readNext();

        assertArrayEquals(expected, found);
        assertArrayEquals(null, cr.readNext());
    }

    public void testBlankLine2() {
        String data = "\r\n";

        KissReader cr = new KissReader(new StringReader(data));

        String[] expected = {""};
        String[] found = cr.readNext();
        assertArrayEquals(expected, found);
        assertArrayEquals(null, cr.readNext());
    }

    public void testBlankLine3() {
        String data = "\r";

        KissReader cr = new KissReader(new StringReader(data));

        String[] expected = {"\r"};
        String[] found = cr.readNext();
        assertArrayEquals(expected, found);
        assertArrayEquals(null, cr.readNext());
    }

    public void testParse1() {

        String[] firstRecord = {"1", "2", "3", "4", "5", "6", "7"};

        String[] secondRecord = {
            "LU",
            "86.25",
            "11/4/1998",
            "2:19PM",
            "His name is \"BOB\"",
            "+4.0625",
            ""
        };

        String data = "1,2,3,4,5,6,7\n"
            + "\"LU\",86.25,\"11/4/1998\",\"2:19PM\",\"His name is \"\"BOB\"\"\",+4.0625, \"\" ";

        KissParser cp = new KissParser(true); // true == trim results
        KissReader cr = new KissReader(new StringReader(data), cp);

        assertArrayEquals(firstRecord, cr.readNext());
        assertArrayEquals(secondRecord, cr.readNext());
        assertArrayEquals(null, cr.readNext());
    }

    public void testParse2() {

        String[] fields = {
            "LU",
            "86.25",
            "11/4/1998",
            "2:19PM",
            "His name is \"BOB\"",
            "+4.0625",
            ""
        };

        String data = "1,2,3,4,5,6,7\n"
            + "\"LU\",86.25,\"11/4/1998\",\"2:19PM\",\"His name is \"\"BOB\"\"\",+4.0625, \"\" " + "\n"
            + "\"LU\",86.25,\"11/4/1998\",\"2:19PM\",\"His name is \"\"BOB\"\"\",+4.0625, \"\" ";

        KissParser cp = new KissParser(true);
        KissReader cr = new KissReader(new StringReader(data), cp);

        String[] first = {"1", "2", "3", "4", "5", "6", "7"};
        assertArrayEquals(first, cr.readNext());

        String[] result = cr.readNext();
        for (int i = 0; i < fields.length; i++) {
            assertEquals(fields[i], result[i]);
        }
        assertArrayEquals(fields, result);

        result = cr.readNext();
        for (int i = 0; i < fields.length; i++) {
            assertEquals(fields[i], result[i]);
        }
        assertArrayEquals(fields, result);
        assertArrayEquals(null, cr.readNext());
    }

    public void testParse3() {

        String[] fields = {
            "LU",
            "86.25",
            "11/4/1998",
            "2:19PM",
            "His name is \"BOB\"",
            "+4.0625",
            ""
        };

        String data = "1,2,3,4,5,6,7\n"
            + "\"LU\"  ,86.25,\"11/4/1998\",\"2:19PM\",\"His name is \"\"BOB\"\"\",+4.0625, \"\"  " + "\n"
            + "\"\"\"\"" + "\n" // ""
            + "\"LU\"  , 86.25 ,\"11/4/1998\" ,\"2:19PM\",\"His name is \"\"BOB\"\"\",+4.0625, \"\" ";

        KissParser cp = new KissParser(true);
        KissReader cr = new KissReader(new StringReader(data), cp);

        String[] first = {"1", "2", "3", "4", "5", "6", "7"};
        assertArrayEquals(first, cr.readNext());

        String[] result = cr.readNext();
        assertArrayEquals(fields, result);

        result = cr.readNext();
        assertArrayEquals(new String[]{"\""}, result);

        result = cr.readNext();
        assertArrayEquals(fields, result);
        assertArrayEquals(null, cr.readNext());
    }
}