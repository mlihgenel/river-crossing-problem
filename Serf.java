import java.util.concurrent.TimeUnit;

public class Serf extends Person implements Runnable {

    public Serf(String ad, Semaphors smphrs) {
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

            // Serf sayısını arttır
            smphrs.serfs++;
            Logger.info(ad + " geldi. Bekleyen: H=" + smphrs.hackers + ", S=" + smphrs.serfs);

            // KOŞUL 1: 4 serf toplanmış mı?
            if (smphrs.serfs == 4) {
                Logger.info(ad + " 🎯 KAPTAN OLDU! 4 serf bulundu, tekne oluşturuluyor...");
                smphrs.serfQueue.release(4);
                smphrs.serfs = 0;
                isCaptain = true;
            }
            // KOŞUL 2: 2 serf + 2 hacker var mı?
            else if (smphrs.serfs == 2 && smphrs.hackers >= 2) {
                Logger.info(ad + " 🎯 KAPTAN OLDU! 2 serf + 2 hacker bulundu, tekne oluşturuluyor...");
                smphrs.serfQueue.release(2);
                smphrs.hackerQueue.release(2);
                smphrs.hackers -= 2;
                smphrs.serfs = 0;
                isCaptain = true;
            }
            // KOŞUL SAĞLANMADI: Bekle
            else {
                Logger.debug(ad + " ⏳ bekliyor (H=" + smphrs.hackers + ", S=" + smphrs.serfs + ") - koşul sağlanmadı");
                smphrs.registerWaitingSerf(ad);
                smphrs.mutex.release();
            }

            // QUEUE'DA BEKLEME - Timeout desteği ile
            Logger.debug(ad + " serfQueue'ya giriyor...");
            boolean queueAcquired;
            if (config.enableTimeout) {
                queueAcquired = smphrs.serfQueue.tryAcquire(config.timeoutMs, TimeUnit.MILLISECONDS);
                if (!queueAcquired) {
                    Logger.error(ad + " ⚠️  TIMEOUT! serfQueue'da çok uzun bekledi! (Olası DEADLOCK!)");
                    smphrs.unregisterWaitingSerf(ad);
                    return;
                }
            } else {
                smphrs.serfQueue.acquire();
                queueAcquired = true;
            }

            // Queue'dan başarıyla geçtik
            smphrs.unregisterWaitingSerf(ad);
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
