package mcmaplib;

import java.io.IOException;

public class NotImplementedException extends IOException {
    protected NotImplementedException(String m) {
        super(m);
    }

    protected NotImplementedException(String m, Throwable e) {
        super(m, e);
    }

    protected NotImplementedException(Throwable e) {
        super(e);
    }

    protected NotImplementedException() {
        super();
    }
}