import java.util.List;

/**
 * Deadlock tespit sınıfı - Arka planda çalışarak deadlock durumunu tespit eder
 */
public class DeadlockDetector extends Thread {

    private Semaphors semaphors;
    private Config config;
    private volatile boolean running = true;
    private volatile boolean deadlockDetected = false;
    private StateSnapshot deadlockState = null;

    public DeadlockDetector(Semaphors semaphors, Config config) {
        this.semaphors = semaphors;
        this.config = config;
        this.setDaemon(true);  // Daemon thread - main thread bitince otomatik kapanır
        this.setName("DeadlockDetector");
    }

    @Override
    public void run() {
        Logger.info("🔍 Deadlock Detector başlatıldı");

        try {
            // İlk kontrol için biraz bekle (thread'ler başlasın)
            Thread.sleep(500);

            while (running) {
                Thread.sleep(1000);  // Her 1 saniyede bir kontrol et

                StateSnapshot currentState = semaphors.getCurrentState();

                // Deadlock kontrolü
                if (isDeadlocked(currentState)) {
                    deadlockDetected = true;
                    deadlockState = currentState;

                    Logger.error("");
                    Logger.error("═".repeat(70));
                    Logger.error("🚨 DEADLOCK TESPİT EDİLDİ! 🚨");
                    Logger.error("═".repeat(70));

                    printDeadlockAnalysis(currentState);

                    running = false;  // Detector'ı durdur
                }
            }

        } catch (InterruptedException e) {
            Logger.debug("DeadlockDetector durduruldu");
        }
    }

    /**
     * Deadlock olup olmadığını kontrol et
     */
    private boolean isDeadlocked(StateSnapshot state) {
        // Durum değişikliğinden bu yana geçen süre
        long timeSinceChange = System.currentTimeMillis() - semaphors.getLastStateChange();

        // Bekleyen thread'ler var mı?
        boolean threadsWaiting = state.totalWaiting() > 0;

        // Tekne oluşturulabilir mi?
        boolean canFormBoat = state.canFormBoat();

        // DEADLOCK KOŞULU:
        // 1. 3+ saniyedir durum değişmemiş VE
        // 2. Thread'ler bekliyor VE
        // 3. Tekne oluşturulamıyor
        boolean isDeadlock = (timeSinceChange > 3000) &&
                             threadsWaiting &&
                             !canFormBoat;

        if (isDeadlock) {
            return true;
        }

        return false;
    }

    /**
     * Deadlock analizi yazdır
     */
    private void printDeadlockAnalysis(StateSnapshot state) {
        Logger.error("");
        Logger.error("📊 MEVCUT DURUM:");
        Logger.error("  Hazırda Bekleyen Hackerlar: " + state.hackers);
        Logger.error("  Hazırda Bekleyen Serfler: " + state.serfs);
        Logger.error("  Queue'da Bekleyen Hackerlar: " + state.waitingHackersCount);
        Logger.error("  Queue'da Bekleyen Serfler: " + state.waitingSerfsCount);
        Logger.error("  Toplam Bekleyen: " + state.totalWaiting());
        Logger.error("");

        // Bekleyen thread detayları
        List<ThreadInfo> waitingHackers = semaphors.getWaitingHackers();
        List<ThreadInfo> waitingSerfs = semaphors.getWaitingSerfs();

        if (!waitingHackers.isEmpty()) {
            Logger.error("⏰ Bekleyen Hackerlar:");
            for (ThreadInfo info : waitingHackers) {
                Logger.error("  - " + info.toString());
            }
        }

        if (!waitingSerfs.isEmpty()) {
            Logger.error("⏰ Bekleyen Serfler:");
            for (ThreadInfo info : waitingSerfs) {
                Logger.error("  - " + info.toString());
            }
        }

        Logger.error("");
        Logger.error("❌ DEADLOCK NEDENİ:");
        Logger.error("  Geçerli tekne kompozisyonları:");
        Logger.error("    ✅ 4 Hacker");
        Logger.error("    ✅ 4 Serf");
        Logger.error("    ✅ 2 Hacker + 2 Serf");
        Logger.error("");
        Logger.error("  Mevcut durum: " + state.hackers + " hacker + " + state.serfs + " serf");

        // Neden tekne oluşturulamıyor?
        if (state.hackers < 4 && state.serfs < 4 &&
            !(state.hackers >= 2 && state.serfs >= 2)) {
            Logger.error("  ❌ Hiçbir geçerli kompozisyon sağlanamıyor!");

            if (state.hackers == 2 && state.serfs == 1) {
                Logger.error("  💡 2H+1S durumu: 1 serf daha gelmeli ama gelmeyecek (thread bitti)");
            } else if (state.hackers == 1 && state.serfs == 2) {
                Logger.error("  💡 1H+2S durumu: 1 hacker daha gelmeli ama gelmeyecek (thread bitti)");
            } else if (state.hackers == 3) {
                Logger.error("  💡 3H durumu: Ya 1 hacker daha veya 2 serf gelmeli");
            } else if (state.serfs == 3) {
                Logger.error("  💡 3S durumu: Ya 1 serf daha veya 2 hacker gelmeli");
            }
        }

        Logger.error("");
        Logger.error("🔧 ÇÖZÜM:");
        Logger.error("  Thread sayılarını dengeleyin veya timeout mekanizması kullanın!");
        Logger.error("  Örnek: FIXED modu ile çalıştırın");
        Logger.error("═".repeat(70));
        Logger.error("");
    }

    /**
     * Detector'ı durdur
     */
    public void stopDetector() {
        running = false;
        this.interrupt();
    }

    /**
     * Deadlock tespit edildi mi?
     */
    public boolean isDeadlockDetected() {
        return deadlockDetected;
    }

    /**
     * Deadlock state'ini al
     */
    public StateSnapshot getDeadlockState() {
        return deadlockState;
    }
}
