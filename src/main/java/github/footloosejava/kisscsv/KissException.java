package github.footloosejava.kisscsv;

public class KissException extends RuntimeException {

    public KissException(String s) {
        super(s);
    }

    public KissException(String s, Exception e) {
        super(s, e);
    }
}
