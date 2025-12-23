import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Semaphors {

    // Mevcut semaforlar (değişmedi)
    public Semaphore mutex       = new Semaphore(1);       // kullanılacak semaforlar
    public Semaphore barrier     = new Semaphore(4);
    public Semaphore hackerQueue = new Semaphore(0);
    public Semaphore serfQueue   = new Semaphore(0);

    public int hackers = 0;                                // hazırda bekleyen hacker sayısı
    public int serfs   = 0;                                // hazırda bekleyen serf sayısı

    // YENİ: Configuration reference
    private Config config;

    // YENİ: Thread takibi
    private List<ThreadInfo> waitingHackers = Collections.synchronizedList(new ArrayList<>());
    private List<ThreadInfo> waitingSerfs = Collections.synchronizedList(new ArrayList<>());

    // YENİ: State history
    private List<StateSnapshot> stateHistory = Collections.synchronizedList(new ArrayList<>());

    // YENİ: Geçiş sayacı
    private AtomicInteger crossingCount = new AtomicInteger(0);

    // YENİ: Son durum değişikliği zamanı
    private volatile long lastStateChange = System.currentTimeMillis();

    /**
     * Constructor - Config ile başlat
     */
    public Semaphors(Config config) {
        this.config = config;
    }

    /**
     * Hacker'ı bekleyen listesine ekle
     */
    public synchronized void registerWaitingHacker(String threadName) {
        ThreadInfo info = new ThreadInfo(threadName, "HACKER");
        waitingHackers.add(info);
        updateStateChange();
    }

    /**
     * Serf'i bekleyen listesine ekle
     */
    public synchronized void registerWaitingSerf(String threadName) {
        ThreadInfo info = new ThreadInfo(threadName, "SERF");
        waitingSerfs.add(info);
        updateStateChange();
    }

    /**
     * Hacker'ı bekleyen listesinden çıkar
     */
    public synchronized void unregisterWaitingHacker(String threadName) {
        waitingHackers.removeIf(info -> info.name.equals(threadName));
        updateStateChange();
    }

    /**
     * Serf'i bekleyen listesinden çıkar
     */
    public synchronized void unregisterWaitingSerf(String threadName) {
        waitingSerfs.removeIf(info -> info.name.equals(threadName));
        updateStateChange();
    }

    /**
     * Geçiş sayısını arttır
     */
    public void incrementCrossingCount() {
        int count = crossingCount.incrementAndGet();
        Logger.info("✅ Geçiş #" + count + " tamamlandı!");
        updateStateChange();
    }

    /**
     * Geçiş sayısını al
     */
    public int getCrossingCount() {
        return crossingCount.get();
    }

    /**
     * Bekleyen hacker sayısı
     */
    public int getWaitingHackersCount() {
        return waitingHackers.size();
    }

    /**
     * Bekleyen serf sayısı
     */
    public int getWaitingSerfsCount() {
        return waitingSerfs.size();
    }

    /**
     * Bekleyen hackerları al
     */
    public synchronized List<ThreadInfo> getWaitingHackers() {
        return new ArrayList<>(waitingHackers);
    }

    /**
     * Bekleyen serfleri al
     */
    public synchronized List<ThreadInfo> getWaitingSerfs() {
        return new ArrayList<>(waitingSerfs);
    }

    /**
     * Mevcut durumu al
     */
    public synchronized StateSnapshot getCurrentState() {
        return new StateSnapshot(this);
    }

    /**
     * Durum değişikliğini kaydet
     */
    private synchronized void updateStateChange() {
        lastStateChange = System.currentTimeMillis();

        // Snapshot oluştur ve kaydet
        StateSnapshot snapshot = getCurrentState();
        stateHistory.add(snapshot);

        // History çok büyümesin (son 100 snapshot)
        if (stateHistory.size() > 100) {
            stateHistory.remove(0);
        }
    }

    /**
     * Son durum değişikliği zamanını al
     */
    public long getLastStateChange() {
        return lastStateChange;
    }

    /**
     * State history'yi al
     */
    public synchronized List<StateSnapshot> getStateHistory() {
        return new ArrayList<>(stateHistory);
    }

    /**
     * Config'i al
     */
    public Config getConfig() {
        return config;
    }
}
