import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Merkezi loglama sınıfı - thread-safe loglama ile zaman damgası ve renk kodlaması
 */
public class Logger {

    private static boolean enabled = true;
    private static LogLevel minLevel = LogLevel.DEBUG;
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    // ANSI renk kodları
    private static final String RESET = "\u001B[0m";
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    // Loglama seviyeleri
    public enum LogLevel {
        DEBUG(0, CYAN),
        INFO(1, GREEN),
        WARNING(2, YELLOW),
        ERROR(3, RED);

        private final int priority;
        private final String color;

        LogLevel(int priority, String color) {
            this.priority = priority;
            this.color = color;
        }

        public int getPriority() {
            return priority;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * Loglama seviyesini ayarla
     */
    public static void setMinLevel(LogLevel level) {
        minLevel = level;
    }

    /**
     * Loglama'yı aktif/pasif yap
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    /**
     * Ana loglama metodu - thread-safe
     */
    public static synchronized void log(LogLevel level, String message) {
        if (!enabled || level.getPriority() < minLevel.getPriority()) {
            return;
        }

        String timestamp = timeFormat.format(new Date());
        String threadName = Thread.currentThread().getName();
        String levelStr = String.format("%-7s", "[" + level + "]");

        // Renk kodlaması ile çıktı
        String color = level.getColor();
        System.out.println(
            color + timestamp + " " +
            levelStr + " " +
            "[" + threadName + "] " +
            message +
            RESET
        );
    }

    /**
     * DEBUG seviyesi log
     */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    /**
     * INFO seviyesi log
     */
    public static void info(String message) {
        log(LogLevel.INFO, message);
    }

    /**
     * WARNING seviyesi log
     */
    public static void warning(String message) {
        log(LogLevel.WARNING, message);
    }

    /**
     * ERROR seviyesi log
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }

    /**
     * Ayırıcı çizgi yazdır
     */
    public static void separator() {
        System.out.println("=".repeat(70));
    }

    /**
     * Başlık yazdır
     */
    public static void header(String title) {
        separator();
        info(title);
        separator();
    }
}
