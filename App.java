public class App {
    public static void main(String[] args) throws InterruptedException {

        // Banner yazdır
        printBanner();

        // Yapılandırma oluştur
        Config config = parseArguments(args);

        // Yapılandırmayı yazdır
        config.print();
        config.printExpectedOutcome();

        // Thread sayıları
        int numberOfHacker = config.numHackers;
        int numberOfSerfs = config.numSerfs;

        // Thread dizileri
        Person[] hackers = new Hacker[numberOfHacker];
        Person[] serfs = new Serf[numberOfSerfs];
        Thread[] thrdHackers = new Thread[numberOfHacker];
        Thread[] thrdSerfs = new Thread[numberOfSerfs];

        // Semaforları tutan sınıftan bir nesne yaratılır
        Semaphors smphrs = new Semaphors(config);

        // DeadlockDetector başlat (eğer aktifse)
        DeadlockDetector detector = null;
        if (config.enableDeadlockDetection) {
            detector = new DeadlockDetector(smphrs, config);
            detector.start();
        }

        Logger.info("");
        Logger.header("🚀 PROGRAM BAŞLADI");

        // Hacker thread'lerini oluştur
        for (int i = 0; i < hackers.length; i++) {
            thrdHackers[i] = new Thread(new Hacker(("hacker_" + i), smphrs));
            thrdHackers[i].setName("hacker_" + i);
        }

        // Serf thread'lerini oluştur
        for (int i = 0; i < serfs.length; i++) {
            thrdSerfs[i] = new Thread(new Serf(("serf___" + i), smphrs));
            thrdSerfs[i].setName("serf___" + i);
        }

        // Thread'leri başlat
        Logger.info("Thread'ler başlatılıyor...");
        for (int i = 0; i < Math.max(serfs.length, hackers.length); i++) {
            if (i < hackers.length)
                thrdHackers[i].start();

            if (i < serfs.length)
                thrdSerfs[i].start();
        }

        Logger.info("✅ " + (numberOfHacker + numberOfSerfs) + " thread başlatıldı");
        Logger.info("");

        // Thread'lerin bitmesini bekle - Timeout ile
        boolean allCompleted = true;
        long startWait = System.currentTimeMillis();
        long maxWait = config.enableTimeout ? config.timeoutMs + 5000 : Long.MAX_VALUE;

        for (int i = 0; i < Math.max(serfs.length, hackers.length); i++) {
            long elapsed = System.currentTimeMillis() - startWait;
            long remaining = maxWait - elapsed;

            if (remaining <= 0) {
                Logger.warning("⏰ Ana thread timeout! Bazı thread'ler hala çalışıyor olabilir");
                allCompleted = false;
                break;
            }

            if (i < hackers.length) {
                thrdHackers[i].join(remaining);
                if (thrdHackers[i].isAlive()) {
                    allCompleted = false;
                }
            }

            if (i < serfs.length) {
                thrdSerfs[i].join(remaining);
                if (thrdSerfs[i].isAlive()) {
                    allCompleted = false;
                }
            }
        }

        // Deadlock detector'ı durdur
        if (detector != null) {
            detector.stopDetector();
        }

        // Sonuç özeti yazdır
        printExecutionSummary(allCompleted, smphrs, detector, config);
    }

    /**
     * Banner yazdır
     */
    private static void printBanner() {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                ║");
        System.out.println("║         🚣  RIVER CROSSING PROBLEM DEMONSTRATION 🚣           ║");
        System.out.println("║                                                                ║");
        System.out.println("║              Hacker & Serf Synchronization                     ║");
        System.out.println("║                                                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println("\n");
    }

    /**
     * Command-line argümanlarını parse et
     */
    private static Config parseArguments(String[] args) {
        // Argüman yoksa varsayılan BALANCED modu
        if (args.length == 0) {
            Logger.info("Mod belirtilmedi, varsayılan: BALANCED");
            return Config.fromMode(Config.Mode.BALANCED);
        }

        // İlk argümanı mode olarak al
        String modeStr = args[0].toUpperCase();

        try {
            Config.Mode mode = Config.Mode.valueOf(modeStr);
            return Config.fromMode(mode);
        } catch (IllegalArgumentException e) {
            Logger.error("Geçersiz mod: " + args[0]);
            Logger.error("Geçerli modlar: BALANCED, DEADLOCK, STARVATION, FIXED");
            Logger.error("Örnek kullanım: java App BALANCED");
            System.exit(1);
            return null;
        }
    }

    /**
     * Yürütme özetini yazdır
     */
    private static void printExecutionSummary(boolean allCompleted, Semaphors smphrs,
                                              DeadlockDetector detector, Config config) {
        Logger.info("");
        Logger.separator();
        Logger.info("📋 YÜRÜTME ÖZETİ");
        Logger.separator();

        int totalCrossings = smphrs.getCrossingCount();
        int expectedCrossings = (config.numHackers + config.numSerfs) / 4;

        Logger.info("Mod: " + config.mode);
        Logger.info("Toplam kişi: " + (config.numHackers + config.numSerfs));
        Logger.info("Beklenen geçiş sayısı: " + expectedCrossings);
        Logger.info("Gerçekleşen geçiş sayısı: " + totalCrossings);

        if (allCompleted && totalCrossings == expectedCrossings) {
            Logger.info("Durum: ✅ BAŞARILI - Tüm kişiler karşıya geçti!");
        } else if (detector != null && detector.isDeadlockDetected()) {
            Logger.error("Durum: ❌ DEADLOCK - Sistem kilitlendi!");
        } else if (!allCompleted) {
            Logger.warning("Durum: ⚠️  KISMEN TAMAMLANDI - Bazı thread'ler timeout oldu");
        } else {
            Logger.warning("Durum: ⚠️  TAMAMLANMADI - " + (expectedCrossings - totalCrossings) + " geçiş eksik");
        }

        // Mod'a özel yorumlar
        Logger.info("");
        switch (config.mode) {
            case BALANCED:
                if (allCompleted && totalCrossings == expectedCrossings) {
                    Logger.info("💡 Dengeli senaryo başarıyla tamamlandı!");
                    Logger.info("   Tüm thread'ler senkronize çalıştı ve sorunsuz geçti.");
                }
                break;

            case DEADLOCK:
                if (detector != null && detector.isDeadlockDetected()) {
                    Logger.info("💡 Deadlock başarıyla gösterildi!");
                    Logger.info("   2H+1S durumunda sistem beklendiği gibi kilitlendi.");
                    Logger.info("   Bu durumda geçerli bir tekne kompozisyonu oluşturulamaz.");
                }
                break;

            case STARVATION:
                Logger.info("💡 Starvation senaryosu gösterildi!");
                Logger.info("   Bekleme sürelerine dikkat edin - bazı thread'ler çok uzun bekledi.");
                break;

            case FIXED:
                Logger.info("💡 Düzeltilmiş versiyon!");
                Logger.info("   Timeout mekanizması sayesinde graceful degradation sağlandı.");
                break;
        }

        Logger.separator();
        System.out.println("\n");
    }
}
