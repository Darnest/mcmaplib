package mcmaplib;

public class InvalidMapException extends Exception {
    protected InvalidMapException(String m) {
        super(m);
    }

    protected InvalidMapException(String m, Throwable e) {
        super(m, e);
    }

    protected InvalidMapException(Throwable e) {
        super(e);
    }

    protected InvalidMapException() {
        super();
    }
}