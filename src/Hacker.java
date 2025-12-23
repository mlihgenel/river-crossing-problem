import java.util.concurrent.TimeUnit;

public class Hacker extends Person implements Runnable {

    public Hacker(String ad, Semaphors smphrs) {
        super(ad, smphrs);
    }

    @Override
    public void run() {
        Config config = smphrs.getConfig();
        long startWait = System.currentTimeMillis();

        try {
            // Rastgele bekleme - thread'lerin farklı zamanlarda gelmesini sağlar
            Thread.sleep((int)(Math.random() * 10));

            Logger.debug(ad + " mutex almaya çalışıyor...");

            // MUTEX ALMA - Timeout desteği ile
            boolean mutexAcquired;
            if (config.enableTimeout) {
                mutexAcquired = smphrs.mutex.tryAcquire(config.timeoutMs, TimeUnit.MILLISECONDS);
                if (!mutexAcquired) {
                    Logger.error(ad + " TIMEOUT! Mutex alınamadı! (Sistem kilitli olabilir)");
                    return;
                }
            } else {
                smphrs.mutex.acquire();
                mutexAcquired = true;
            }

            Logger.debug(ad + " mutex aldı. Mevcut durum: H=" + smphrs.hackers + ", S=" + smphrs.serfs);

            // Hacker sayısını arttır
            smphrs.hackers++;
            Logger.info(ad + " geldi. Bekleyen: H=" + smphrs.hackers + ", S=" + smphrs.serfs);

            // KOŞUL 1: 4 hacker toplanmış mı?
            if (smphrs.hackers == 4) {
                Logger.info(ad + " 🎯 KAPTAN OLDU! 4 hacker bulundu, tekne oluşturuluyor...");
                smphrs.hackerQueue.release(4);
                smphrs.hackers = 0;
                isCaptain = true;
            }
            // KOŞUL 2: 2 hacker + 2 serf var mı?
            else if (smphrs.hackers == 2 && smphrs.serfs >= 2) {
                Logger.info(ad + " 🎯 KAPTAN OLDU! 2 hacker + 2 serf bulundu, tekne oluşturuluyor...");
                smphrs.hackerQueue.release(2);
                smphrs.serfQueue.release(2);
                smphrs.serfs -= 2;
                smphrs.hackers = 0;
                isCaptain = true;
            }
            // KOŞUL SAĞLANMADI: Bekle
            else {
                Logger.debug(ad + " ⏳ bekliyor (H=" + smphrs.hackers + ", S=" + smphrs.serfs + ") - koşul sağlanmadı");
                smphrs.registerWaitingHacker(ad);
                smphrs.mutex.release();
            }

            // QUEUE'DA BEKLEME - Timeout desteği ile
            Logger.debug(ad + " hackerQueue'ya giriyor...");
            boolean queueAcquired;
            if (config.enableTimeout) {
                queueAcquired = smphrs.hackerQueue.tryAcquire(config.timeoutMs, TimeUnit.MILLISECONDS);
                if (!queueAcquired) {
                    Logger.error(ad + " ⚠️  TIMEOUT! hackerQueue'da çok uzun bekledi! (Olası DEADLOCK!)");
                    smphrs.unregisterWaitingHacker(ad);
                    return;
                }
            } else {
                smphrs.hackerQueue.acquire();
                queueAcquired = true;
            }

            // Queue'dan başarıyla geçtik
            smphrs.unregisterWaitingHacker(ad);
            long waitTime = System.currentTimeMillis() - startWait;
            Logger.info(ad + " 🚢 tekneye bindi! (Bekleme süresi: " + waitTime + "ms)");

            // Starvation uyarısı
            if (waitTime > 2000 && config.mode == Config.Mode.STARVATION) {
                Logger.warning(ad + " ⚠️  STARVATION: " + waitTime + "ms gibi uzun bir süre bekledi!");
            }

            // Board fonksiyonu
            Board();

            // BARRIER - 4 kişi toplanana kadar bekle
            smphrs.barrier.acquire();

            // KAPTAN işlemleri
            if (this.isCaptain) {
                Thread.sleep(1000);  // Gözlemlemek için bekle
                rowBoat();
                smphrs.barrier.release(4);    // Sonraki grup için barrier'ı serbest bırak
                smphrs.mutex.release();       // Sonraki grubun başlaması için mutex'i serbest bırak
            }

        } catch (InterruptedException e) {
            Logger.error(ad + " kesintiye uğradı: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Logger.error(ad + " beklenmeyen hata: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
