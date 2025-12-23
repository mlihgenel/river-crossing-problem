/**
 * Sistem durumunun anlık görüntüsü - immutable snapshot
 */
public class StateSnapshot {
    public final long timestamp;
    public final int hackers;
    public final int serfs;
    public final int crossingCount;
    public final int waitingHackersCount;
    public final int waitingSerfsCount;

    /**
     * Semaphors nesnesinden snapshot oluştur
     */
    public StateSnapshot(Semaphors s) {
        this.timestamp = System.currentTimeMillis();
        this.hackers = s.hackers;
        this.serfs = s.serfs;
        this.crossingCount = s.getCrossingCount();
        this.waitingHackersCount = s.getWaitingHackersCount();
        this.waitingSerfsCount = s.getWaitingSerfsCount();
    }

    /**
     * Manuel snapshot oluştur
     */
    public StateSnapshot(int hackers, int serfs, int crossingCount,
                         int waitingHackersCount, int waitingSerfsCount) {
        this.timestamp = System.currentTimeMillis();
        this.hackers = hackers;
        this.serfs = serfs;
        this.crossingCount = crossingCount;
        this.waitingHackersCount = waitingHackersCount;
        this.waitingSerfsCount = waitingSerfsCount;
    }

    /**
     * Tekne oluşturulabilir mi?
     */
    public boolean canFormBoat() {
        return (hackers >= 4) ||
               (serfs >= 4) ||
               (hackers >= 2 && serfs >= 2);
    }

    /**
     * Toplam bekleyen thread sayısı
     */
    public int totalWaiting() {
        return waitingHackersCount + waitingSerfsCount;
    }

    /**
     * String gösterimi
     */
    @Override
    public String toString() {
        return String.format(
            "H=%d, S=%d, Geçişler=%d, Bekleyen(H=%d, S=%d), TekneOluşturulabilir=%s",
            hackers, serfs, crossingCount,
            waitingHackersCount, waitingSerfsCount,
            canFormBoat() ? "EVET" : "HAYIR"
        );
    }

    /**
     * Detaylı string gösterimi
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Durum Anlık Görüntüsü ===\n");
        sb.append("Zaman: ").append(new java.util.Date(timestamp)).append("\n");
        sb.append("Hazırda Bekleyen Hackerlar: ").append(hackers).append("\n");
        sb.append("Hazırda Bekleyen Serfler: ").append(serfs).append("\n");
        sb.append("Tamamlanan Geçiş Sayısı: ").append(crossingCount).append("\n");
        sb.append("Queue'da Bekleyen Hackerlar: ").append(waitingHackersCount).append("\n");
        sb.append("Queue'da Bekleyen Serfler: ").append(waitingSerfsCount).append("\n");
        sb.append("Tekne Oluşturulabilir mi? ").append(canFormBoat() ? "EVET" : "HAYIR").append("\n");

        if (!canFormBoat() && totalWaiting() > 0) {
            sb.append("⚠️  UYARI: Thread'ler bekliyor ama tekne oluşturulamıyor! (Olası deadlock)\n");
        }

        return sb.toString();
    }
}
