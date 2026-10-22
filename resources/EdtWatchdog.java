package resources;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

/**
 * Notices when the event thread stops responding and writes down what every
 * thread was doing at the time.
 *
 * A frozen Swing app shows the user a spinning cursor and nothing else. By the
 * time anyone can attach a debugger the moment has passed, and a bug that will
 * not reproduce on demand cannot be fixed. So once a second this pings the
 * event thread; if a ping goes unanswered for STALL_MS it dumps all thread
 * stacks to a file in the temp directory, once per stall, and says where on
 * stderr. Whoever hits the freeze then has the file to send along.
 */
final class EdtWatchdog {

    private static final long PING_MS = 1000;
    private static final long STALL_MS = 3000;

    private EdtWatchdog() {
    }

    static void start() {
        Thread t = new Thread(EdtWatchdog::run, "cantordust-edt-watchdog");
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    private static void run() {
        while (true) {
            try {
                Thread.sleep(PING_MS);
                CountDownLatch answered = new CountDownLatch(1);
                SwingUtilities.invokeLater(answered::countDown);
                if (answered.await(STALL_MS, TimeUnit.MILLISECONDS)) {
                    continue;
                }
                // Stalled. Record it once, then wait out the rest of the stall so
                // a long freeze does not produce a file per second.
                Path file = dump();
                System.err.println("cantordust: event thread unresponsive for " + STALL_MS + "ms; thread dump written to " + file);
                answered.await();
            } catch (InterruptedException e) {
                return;
            } catch (RuntimeException e) {
                // The watchdog must never take the app down with it.
            }
        }
    }

    private static Path dump() {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = Path.of(System.getProperty("java.io.tmpdir"), "cantordust-hang-" + stamp + ".txt");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            out.println("Cantordust: event dispatch thread unresponsive for " + STALL_MS + "ms at " + stamp);
            out.println("java " + System.getProperty("java.version") + " on " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            out.println();
            for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                Thread th = e.getKey();
                out.println("\"" + th.getName() + "\" " + (th.isDaemon() ? "daemon " : "") + "prio=" + th.getPriority() + " " + th.getState());
                for (StackTraceElement s : e.getValue()) {
                    out.println("        at " + s);
                }
                out.println();
            }
        } catch (IOException e) {
            System.err.println("cantordust: could not write thread dump: " + e);
        }
        return file;
    }
}
