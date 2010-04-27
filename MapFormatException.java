package mcmaplib;

import java.io.IOException;

public class MapFormatException extends IOException {
    protected MapFormatException(String m) {
        super(m);
    }

    protected MapFormatException(String m, Throwable e) {
        super(m, e);
    }

    protected MapFormatException(Throwable e) {
        super(e);
    }

    protected MapFormatException() {
        super();
    }
}