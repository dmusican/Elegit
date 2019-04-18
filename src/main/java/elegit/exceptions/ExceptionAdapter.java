package elegit.exceptions;

// http://www.mindview.net/Etc/Discussions/CheckedExceptions
import net.jcip.annotations.ThreadSafe;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

@ThreadSafe
// All state is either final, or atomic
public class ExceptionAdapter extends RuntimeException {

    // Used for automated tests that fail to catch actual exceptions
    private static AtomicInteger exceptionsWrapped = new AtomicInteger(0);

    private final String stackTrace;

    private final Throwable originalException;

    public ExceptionAdapter(Throwable e) {
        super(e.toString());
        originalException = e;
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        stackTrace = sw.toString();
        exceptionsWrapped.getAndIncrement();
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(java.io.PrintStream s) {
        synchronized(s) {
            s.print(getClass().getName() + ": ");
            s.print(stackTrace);
        }
    }
    public void printStackTrace(java.io.PrintWriter s) {
        synchronized(s) {
            s.print(getClass().getName() + ": ");
            s.print(stackTrace);
        }
    }
    public void rethrow() throws Throwable {
        throw originalException;
    }

    public static int getWrappedCount() {
        return exceptionsWrapped.get();
    }
}