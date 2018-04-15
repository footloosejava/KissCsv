KissCsv
=======

A **very fast, standards compliant and threadsafe** CSV library with only a few, essential, configurable features.

This is a major overhaul and reworking of [Michael Peterson's SimpleCsv ](https://github.com/quux00/simplecsv). It is released under the same Apache Version 2.0 License.

KissCsv is a fully mime (text/csv) compliant parser according to [http://tools.ietf.org/html/rfc4180](http://tools.ietf.org/html/rfc4180) 

## Why make another csv parser?

Both `OpenCSV` and `SimpleCSV` had too many features for me. So I decided to make one that is compliant, faster and ... threadsafe. Along the way I fixed a few features:

- In most csv data, whitepace outside of the quoted value is to be ignored. `OpenCSV` was missing the ability to trim whitespace 
after a closing quote and before the next separator. 

- `SimpleCsv` was missing embedded carriage returns and doubled-escaped quotes within quoted values.

**KissCsv can be configured with:**
 - custom field separator
 - custom quote character
 - setting to trim all fields of whitespace (even quoted fields).
 
## To get this Git project into your build:
 
Follow instructions at [https://jitpack.io/#footloosejava/kisscsv](https://jitpack.io/#footloosejava/kisscsv) or these steps for Maven:
 
Step 1. Add the JitPack repository to your build file

```
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
Step 2. Add the dependency

```
<dependency>
    <groupId>com.github.footloosejava</groupId>
    <artifactId>kisscsv</artifactId>
    <version>v1.1</version>
</dependency>
```

## What You Need to Know:

1) If a quote character is specified, whitespace outside of a quote is ignored.

      With **`"`** quote character set, `A,B, "HELLO"  ,D` is parsed as `A,B,HELLO,D`
      
      With **`null`** quote character set, `A,B, "HELLO"  ,D` is parsed **as is**.
      
2) KissCsv properly handles embedded carriage returns in quoted values:

      `A,B,  "HELLO\r\nWORLD"   ,D` is parsed as `A,B,HELLO\r\nWORLD,D` 

3) KissCsv properly recognizes `\r` or `\r\n` as record endings. These two are parsed as 2 records, consisting of 4 fields each:

      `A,B, "HELLO\r\nWORLD"  ,D`[CR]`E,F,G,H`
      
      `A,B, "HELLO\r\nWORLD"  ,D`[CR+LF]`E,F,G,H`      

4) KissCsv meets a simple use scenario - it does not recognize or use any escape characters.

5) KissCsv recognizes doubled quotes `""` as single quotes inside a quoted value:

      `"A quote "" is what a quote is!"` is parsed as `A quote " is what a quote is!`
      
6) KissCsv parser complains if a quote character is set and fields are malformed:

      a) Non-whitespace found before opening quote. `Hello"World"` will 
      throw a KissException: `Character found before first quote`.
    
      b) Non-whitespace found after closing quote. `"HELLO" WORLD` will 
      throw a KissException: `Record has already been closed (matched quotes found)`

8) KissCsv `KissParser` is memory-frugal, threadsafe and very fast.

## Example - Reading a `String[]`

The default KissParser constructor has the following settings:
- separator = `','`
- quote character = `'"'`
- trim fields = `false`

```
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

// The KissParser can be configured once and then used with any number of KissReaders simultaneously.
KissParser cp = new KissParser();

// Just like Bufferedreader, null is returned when there is no more data.
KissReader cr = new KissReader(new StringReader(data), cp);

assertArrayEquals(firstRecord, cr.readNext());
assertArrayEquals(secondRecord, cr.readNext());
assertArrayEquals(null, cr.readNext());
```

## Example - Using a Consumer to Process Data

Processing record data using a consumer without String[] creation.
```
String data = "1,2,3,4,5";
KissParser cp = new KissParser();
KissReader cr = new KissReader(new StringReader(data), cp);

StringBuilder sb = new StringBuilder();
assertEquals(5, cr.readNext(sb::append));
assertEquals("12345", sb.toString());
```

If EOF, the consumer is never called and -1 is returned.
```
String data = "";
KissParser cp = new KissParser();
KissReader cr = new KissReader(new StringReader(data), cp);

StringBuilder sb = new StringBuilder();
assertEquals(-1, cr.readNext(sb::append));
assertEquals("", sb.toString());
}
```
