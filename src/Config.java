/**
 * Yapılandırma sınıfı - Farklı demonstrasyon modları için ayarlar
 */
public class Config {

    // Demonstrasyon modları
    public enum Mode {
        BALANCED,      // Dengeli senaryo - her şey sorunsuz çalışır
        DEADLOCK,      // Deadlock gösterimi - program kilitlenir
        STARVATION,    // Starvation gösterimi - bazı thread'ler uzun bekler
        FIXED          // Düzeltilmiş versiyon - tüm sorunlar çözülmüş
    }

    public Mode mode;
    public int numHackers;
    public int numSerfs;
    public boolean enableTimeout;
    public long timeoutMs;
    public boolean enableDeadlockDetection;
    public boolean enableDetailedLogging;
    public boolean fairnessEnabled;

    /**
     * Constructor - varsayılan değerler
     */
    public Config() {
        this.mode = Mode.BALANCED;
        this.numHackers = 8;
        this.numSerfs = 8;
        this.enableTimeout = false;
        this.timeoutMs = 5000;
        this.enableDeadlockDetection = false;
        this.enableDetailedLogging = true;
        this.fairnessEnabled = false;
    }

    /**
     * Mod'a göre yapılandırma oluştur
     */
    public static Config fromMode(Mode mode) {
        Config config = new Config();
        config.mode = mode;

        switch (mode) {
            case BALANCED:
                // Dengeli senaryo - 8 hacker + 8 serf
                config.numHackers = 8;
                config.numSerfs = 8;
                config.enableTimeout = false;
                config.enableDeadlockDetection = false;
                config.enableDetailedLogging = true;
                config.fairnessEnabled = false;
                Logger.info("BALANCED Modu: Dengeli senaryo (8H + 8S), sorunsuz çalışacak");
                break;

            case DEADLOCK:
                // Deadlock senaryosu - 2 hacker + 1 serf (tekne oluşturulamaz)
                config.numHackers = 2;
                config.numSerfs = 1;
                config.enableTimeout = true;
                config.timeoutMs = 5000;  // 5 saniye timeout
                config.enableDeadlockDetection = true;
                config.enableDetailedLogging = true;
                config.fairnessEnabled = false;
                Logger.warning("DEADLOCK Modu: Dengesiz senaryo (2H + 1S), deadlock oluşacak!");
                break;

            case STARVATION:
                // Starvation senaryosu - 10 hacker + 2 serf
                config.numHackers = 10;
                config.numSerfs = 2;
                config.enableTimeout = false;
                config.enableDeadlockDetection = false;
                config.enableDetailedLogging = true;
                config.fairnessEnabled = false;
                Logger.warning("STARVATION Modu: Dengesiz senaryo (10H + 2S), serfler açlığa maruz kalacak!");
                break;

            case FIXED:
                // Düzeltilmiş versiyon - 2 hacker + 1 serf ama timeout ile
                config.numHackers = 2;
                config.numSerfs = 1;
                config.enableTimeout = true;
                config.timeoutMs = 10000;  // 10 saniye timeout
                config.enableDeadlockDetection = true;
                config.enableDetailedLogging = true;
                config.fairnessEnabled = true;
                Logger.info("FIXED Modu: Güvenlik mekanizmaları aktif, graceful degradation");
                break;
        }

        return config;
    }

    /**
     * Yapılandırmayı yazdır
     */
    public void print() {
        Logger.separator();
        Logger.info("=== YAPILANDIRMA ===");
        Logger.info("Mod: " + mode);
        Logger.info("Hacker sayısı: " + numHackers);
        Logger.info("Serf sayısı: " + numSerfs);
        Logger.info("Toplam kişi: " + (numHackers + numSerfs));
        Logger.info("Timeout aktif: " + enableTimeout);
        if (enableTimeout) {
            Logger.info("Timeout süresi: " + timeoutMs + "ms");
        }
        Logger.info("Deadlock tespiti: " + enableDeadlockDetection);
        Logger.info("Detaylı loglama: " + enableDetailedLogging);
        Logger.info("Fairness: " + fairnessEnabled);
        Logger.separator();
    }

    /**
     * Beklenen sonucu yazdır
     */
    public void printExpectedOutcome() {
        Logger.info("");
        Logger.info("=== BEKLENEN SONUÇ ===");
        switch (mode) {
            case BALANCED:
                int trips = (numHackers + numSerfs) / 4;
                Logger.info("✅ " + trips + " sefer yapılacak");
                Logger.info("✅ Tüm " + (numHackers + numSerfs) + " kişi sorunsuz geçecek");
                Logger.info("✅ Her tekne tam 4 kişi ile dolacak");
                break;

            case DEADLOCK:
                Logger.warning("❌ Deadlock oluşacak!");
                Logger.warning("❌ 2H + 1S ile geçerli tekne oluşturulamaz");
                Logger.warning("❌ Thread'ler sonsuz bekleyecek");
                Logger.warning("⏰ " + (timeoutMs/1000) + " saniye sonra timeout ile kapanacak");
                break;

            case STARVATION:
                Logger.warning("⚠️  Hackerlar öncelikli geçecek");
                Logger.warning("⚠️  Serfler uzun süre bekleyecek (starvation)");
                Logger.warning("⚠️  Unfair scheduling görülecek");
                break;

            case FIXED:
                Logger.info("✅ Timeout mekanizması aktif");
                Logger.info("✅ Deadlock tespit edilecek");
                Logger.info("✅ Graceful shutdown yapılacak");
                Logger.info("⏰ Maksimum " + (timeoutMs/1000) + " saniye beklenecek");
                break;
        }
        Logger.separator();
        Logger.info("");
    }
}
