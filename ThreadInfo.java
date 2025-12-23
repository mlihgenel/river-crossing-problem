/**
 * Thread bilgilerini takip eden sınıf
 */
public class ThreadInfo {
    public final String name;
    public final long arrivalTime;
    public final String type;  // "HACKER" veya "SERF"

    /**
     * Constructor
     */
    public ThreadInfo(String name, String type) {
        this.name = name;
        this.type = type;
        this.arrivalTime = System.currentTimeMillis();
    }

    /**
     * Bekleme süresini hesapla (milisaniye)
     */
    public long getWaitTime() {
        return System.currentTimeMillis() - arrivalTime;
    }

    /**
     * Bekleme süresini saniye cinsinden al
     */
    public double getWaitTimeSeconds() {
        return getWaitTime() / 1000.0;
    }

    /**
     * String gösterimi
     */
    @Override
    public String toString() {
        return String.format("%s (%s) - %.2f saniye bekliyor",
                           name, type, getWaitTimeSeconds());
    }

    /**
     * Eşitlik kontrolü (isim bazlı)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ThreadInfo other = (ThreadInfo) obj;
        return name.equals(other.name);
    }

    /**
     * Hash code (isim bazlı)
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
