# River Crossing Problem - İleri Seviye Teknik Dokümantasyon

## İçindekiler

1. [Problem Tanımı](#1-problem-tanımı)
2. [Kısıtlamalar ve Kurallar](#2-kısıtlamalar-ve-kurallar)
3. [Algoritma Açıklaması](#3-algoritma-açıklaması)
4. [Kod Mimarisi](#4-kod-mimarisi)
5. [Senkronizasyon Mekanizmaları](#5-senkronizasyon-mekanizmaları)
6. [Deadlock Analizi](#6-deadlock-analizi)
7. [Starvation Analizi](#7-starvation-analizi)
8. [Race Condition'lar](#8-race-conditionlar)
9. [Çözümler ve İyileştirmeler](#9-çözümler-ve-iyileştirmeler)
10. [Teorik Arka Plan](#10-teorik-arka-plan)
11. [Programı Çalıştırma](#11-programı-çalıştırma)
12. [İleri Seviye Konular](#12-ileri-seviye-konular)

---

## 1. Problem Tanımı

### 1.1 River Crossing Problem Nedir?

River Crossing Problem, klasik bir senkronizasyon ve koordinasyon problemidir. Bu implementasyonda iki tip entity vardır: **Hacker'lar** ve **Serf'ler**. Her iki grup da bir nehri geçmek istemektedir, ancak bunu yaparken belirli kurallara uymaları gerekmektedir.

### 1.2 Senaryo

Bir nehrin bir kıyısında Hacker'lar ve Serf'ler beklemektedir. Karşıya geçmek için bir tekne kullanılmalıdır. Teknenin kapasitesi sabittir ve belirli kompozisyon kurallarına uyulmalıdır.

### 1.3 Tekne Özellikleri

- **Sabit Kapasite**: Tekne her seferinde **tam 4 kişi** alabilir
- **Otomatik Dönüş**: Her geçiş sonrası tekne otomatik olarak başlangıç noktasına geri döner
- **Çoklu Sefer**: Tüm insanlar geçene kadar sefer tekrarlanır
  
**Örnek**: 16 kişi (8 hacker + 8 serf) için → 4 sefer gerekir (16 ÷ 4 = 4)

---

## 2. Kısıtlamalar ve Kurallar

### 2.1 Tekne Kompozisyon Kuralları

#### Geçerli Kompozisyonlar (✅)

1. **4 Hacker** (Homojen Grup)
   - Sadece hackerlardan oluşan tam bir tekne
   - Örnek: `[H, H, H, H]`

2. **4 Serf** (Homojen Grup)
   - Sadece serflerden oluşan tam bir tekne
   - Örnek: `[S, S, S, S]`

3. **2 Hacker + 2 Serf** (Heterojen Grup)
   - Dengeli karışık kompozisyon
   - Örnek: `[H, H, S, S]`

#### Geçersiz Kompozisyonlar (❌)

1. **3 Hacker + 1 Serf** (Asimetrik)
   - Hacker çoğunluğu kabul edilmez
   
2. **1 Hacker + 3 Serf** (Asimetrik)
   - Serf çoğunluğu kabul edilmez

3. **0-3 Kişilik Gruplar**
   - Tekne tam 4 kişi ile dolu olmalı

### 2.2 Senkronizasyon Gereksinimleri

1. **Mutual Exclusion**: Aynı anda sadece bir thread tekne kompozisyonunu kontrol edebilir
2. **Barrier Synchronization**: 4 kişi toplanana kadar beklenilmeli
3. **Queue Management**: Bekleyen thread'ler sıralı bir şekilde yönetilmeli
4. **Captain Selection**: Bir kişi "kaptan" olarak seçilmeli ve rowBoat() fonksiyonunu çağırmalı

---

## 3. Algoritma Açıklaması

### 3.1 Genel Akış

```
┌─────────────┐
│ Thread Başla│
└──────┬──────┘
       │
       v
┌──────────────────┐
│ Mutex Acquire    │ ← Kritik bölgeye giriş
└──────┬───────────┘
       │
       v
┌──────────────────┐
│ Sayacı Arttır    │ (hackers++ veya serfs++)
└──────┬───────────┘
       │
       v
    ┌──┴───┐
    │Koşul?│
    └──┬───┘
       │
   ┌───┴────┬─────────┬──────────┐
   │        │         │          │
   v        v         v          v
 4H?    4S?   2H+2S?    Bekle
   │        │         │          │
   └────┬───┴─────┬───┘          │
        │         │              │
        v         v              v
   ┌─────────────────┐    ┌──────────────┐
   │ Queue Release   │    │ Mutex Release│
   │ Captain = true  │    │ Queue Bekle  │
   └────────┬────────┘    └──────┬───────┘
            │                    │
            v                    v
    ┌────────────────┐    ┌──────────────┐
    │ Queue'ya Git   │    │ (Blocked)    │
    └────────┬───────┘    └──────────────┘
             │
             v
    ┌────────────────┐
    │ Tekneye Bin    │
    └────────┬───────┘
             │
             v
    ┌────────────────┐
    │ Barrier Bekle  │ ← 4 kişi toplanana kadar bekle
    └────────┬───────┘
             │
       ┌─────┴─────┐
       │           │
   Kaptan?      Değil
       │           │
       v           v
   rowBoat()    (Bekle)
       │           │
   Release       Thread
   Barrier       Biter
       │
   Release
   Mutex
       │
       v
   Thread Biter
```

### 3.2 Detaylı Adım Adım İzleme

#### Senaryo: 8 Hacker + 8 Serf

**Sefer 1: 4 Hacker Geçişi**

```
t=0ms:  hacker_0 gelir → H=1, S=0 (bekle)
t=1ms:  hacker_1 gelir → H=2, S=0 (bekle)
t=2ms:  hacker_2 gelir → H=3, S=0 (bekle)
t=3ms:  hacker_3 gelir → H=4, S=0 → KAPTAN! (4H koşulu sağlandı)
        → hackerQueue.release(4)
        → hackers = 0
        
t=4ms:  4 hacker tekneye biner
t=1004ms: hacker_3 (kaptan) rowBoat() çağırır
          → barrier.release(4)
          → mutex.release()
          
Geçiş #1 Tamamlandı!
```

**Sefer 2: 4 Hacker Daha**

```
t=1005ms: hacker_4 gelir → H=1, S=0 (bekle)
t=1006ms: hacker_5 gelir → H=2, S=0 (bekle)
t=1007ms: hacker_6 gelir → H=3, S=0 (bekle)
t=1008ms: hacker_7 gelir → H=4, S=0 → KAPTAN!

... (aynı süreç)

Geçiş #2 Tamamlandı!
```

**Sefer 3: 2 Hacker + 2 Serf**

```
t=2010ms: serf___0 gelir → H=0, S=1 (bekle)
t=2011ms: serf___1 gelir → H=0, S=2 (bekle)
t=2012ms: hacker_8 gelir → H=1, S=2 (bekle)
t=2013ms: hacker_9 gelir → H=2, S=2 → KAPTAN! (2H+2S koşulu sağlandı)
          → hackerQueue.release(2)
          → serfQueue.release(2)
          → serfs -= 2
          → hackers = 0

... (4 kişi biner)

Geçiş #3 Tamamlandı!
```

---

## 4. Kod Mimarisi

### 4.1 Sınıf Diyagramı

```
┌──────────────┐
│    App       │  ← Main entry point
└──────┬───────┘
       │
       ├─────► ┌──────────────┐
       │       │   Config     │  ← Yapılandırma
       │       └──────────────┘
       │
       ├─────► ┌──────────────┐
       │       │  Semaphors   │  ← Semaforlar ve state
       │       └──────────────┘
       │
       ├─────► ┌─────────────────────┐
       │       │ DeadlockDetector    │  ← Monitoring
       │       └─────────────────────┘
       │
       └─────► ┌──────────────┐
               │   Person     │  ← Base class
               └──────┬───────┘
                      │
           ┌──────────┴──────────┐
           │                     │
    ┌──────▼──────┐      ┌──────▼──────┐
    │   Hacker    │      │    Serf     │
    └─────────────┘      └─────────────┘
```

### 4.2 Sınıf Detayları

#### 4.2.1 App.java
**Sorumluluk**: Ana program, orkestrasyon

**Kritik Metodlar**:
- `main()`: Entry point
- `parseArguments()`: Command-line parsing
- `printExecutionSummary()`: Sonuç analizi

**Akış**:
1. Banner yazdır
2. Config oluştur (mod'a göre)
3. Semaphors ve DeadlockDetector başlat
4. Thread'leri oluştur ve başlat
5. Join ile bekle (timeout ile)
6. Özet yazdır

#### 4.2.2 Config.java
**Sorumluluk**: Yapılandırma yönetimi

**Modlar**:
- `BALANCED`: 8H+8S, timeout yok
- `DEADLOCK`: 2H+1S, 5s timeout, detector aktif
- `STARVATION`: 10H+2S, unfairness gösterimi
- `FIXED`: 2H+1S, 10s timeout, graceful degradation

#### 4.2.3 Semaphors.java
**Sorumluluk**: Semafor ve state yönetimi

**Semaforlar**:
- `mutex`: Mutual exclusion (1 permit)
- `barrier`: Barrier synchronization (4 permits)
- `hackerQueue`: Hacker bekle kuyruğu (0 permit)
- `serfQueue`: Serf bekle kuyruğu (0 permit)

**State Tracking**:
- `hackers`, `serfs`: Hazırda bekleyen sayılar
- `waitingHackers`, `waitingSerfs`: Queue'da bekleyenler
- `stateHistory`: Durum geçmişi
- `crossingCount`: Geçiş sayacı

---

## 5. Senkronizasyon Mekanizmaları

### 5.1 Mutex Semaforu (Kritik Bölge Koruması)

**Amaç**: Sayaç güncellemelerinde race condition önlemek

**Kullanım**:
```java
smphrs.mutex.acquire();  // Kritik bölgeye gir
try {
    smphrs.hackers++;     // Atomik güncelleme
    // ... koşul kontrolleri ...
} finally {
    smphrs.mutex.release(); // Kritik bölgeden çık
}
```

**Neden Gerekli?**:
- Eğer mutex olmasaydı, iki thread aynı anda `hackers++` yapabilirdi
- Bu da lost update problemine yol açardı

**Örnek Race Condition** (mutex olmadan):
```
Thread A: oku(hackers) → 2
Thread B: oku(hackers) → 2
Thread A: yaz(hackers=3)
Thread B: yaz(hackers=3)
Sonuç: hackers=3 (olması gereken: 4)
```

### 5.2 Queue Semaforları (Bekleme Kuyruğu)

**Amaç**: Thread'leri bloklayıp uyandırmak

**Mechanism**:
```java
// Blocking wait (0 permit ile başlatılmış)
smphrs.hackerQueue.acquire();  // Thread bloklanır

// Wake up (başka thread tarafından)
smphrs.hackerQueue.release(4);  // 4 thread'i uyandır
```

**Timeout Desteği** (DEADLOCK ve FIXED modlarında):
```java
boolean success = smphrs.hackerQueue.tryAcquire(
    config.timeoutMs, 
    TimeUnit.MILLISECONDS
);

if (!success) {
    // Timeout! Deadlock olabilir
    Logger.error("TIMEOUT - Deadlock detected!");
    return;
}
```

### 5.3 Barrier Semaforu (4 Kişi Senkronizasyonu)

**Amaç**: 4 kişinin hep birlikte ilerlemesini sağlamak

**Problem**: Counting semaphore kullanımı (ideal değil!)

**Mevcut Implementasyon**:
```java
// Başlangıçta 4 permit
Semaphore barrier = new Semaphore(4);

// Her thread acquire yapar
barrier.acquire();  // Permit azalır

// Kaptan barrier'ı reset eder
barrier.release(4);  // 4 permit geri verir
```

**Neden Sorunlu?**:
- Bu **gerçek bir barrier değil**, counting semaphore
- Eğer kaptan `release(4)` yapmazsa, sonraki grup sonsuz bekler
- CyclicBarrier daha uygun olurdu

**Alternatif (İdeal)**:
```java
CyclicBarrier barrier = new CyclicBarrier(4, () -> {
    // Barrier action - 4 kişi toplandığında otomatik çalışır
    System.out.println("Tekne dolu!");
});

// Her thread
barrier.await();  // 4 kişi toplanana kadar bekle
```

---

## 6. Deadlock Analizi

### 6.1 Deadlock Nedir?

**Tanım**: Sistem durumunun hiçbir thread'in ilerleyemediği bir duruma gelmesi.

**Coffman Koşulları** (4'ü de sağlanmalı):
1. **Mutual Exclusion**: Kaynaklar exclusive kullanılıyor ✅
2. **Hold and Wait**: Thread'ler kaynak tutup başka kaynak bekliyor ✅
3. **No Preemption**: Kaynaklar zorla alınamıyor ✅
4. **Circular Wait**: Döngüsel bekleme var mı? ✅

### 6.2 Deadlock Senaryosu (2H + 1S)

**Başlangıç Durumu**:
```
Hackerlar: hacker_0, hacker_1
Serfler: serf___0
```

**Zaman Çizelgesi**:

```
t=0ms:
hacker_0 gelir
mutex.acquire() ✓
hackers = 1
Koşul kontrol: 1H + 0S → Hiçbiri sağlanmaz
mutex.release()
hackerQueue.acquire() → BLOCKED 🔒

t=1ms:
hacker_1 gelir
mutex.acquire() ✓
hackers = 2
Koşul kontrol: 2H + 0S → Hiçbiri sağlanmaz
mutex.release()
hackerQueue.acquire() → BLOCKED 🔒

t=2ms:
serf___0 gelir
mutex.acquire() ✓
serfs = 1
Koşul kontrol: 2H + 1S → Hiçbiri sağlanmaz
  - 4H? NO (sadece 2H var)
  - 4S? NO (sadece 1S var)
  - 2H+2S? NO (2H var ama sadece 1S var, 2S gerekli)
mutex.release()
serfQueue.acquire() → BLOCKED 🔒
```

**Sonuç**:
```
┌─────────────┐
│ hacker_0    │ ──> hackerQueue'da BLOCKED
└─────────────┘

┌─────────────┐
│ hacker_1    │ ──> hackerQueue'da BLOCKED
└─────────────┘

┌─────────────┐
│ serf___0    │ ──> serfQueue'da BLOCKED
└─────────────┘

Durum: hackers=2, serfs=1
Hiçbir geçerli kompozisyon yok!
❌ DEADLOCK!
```

### 6.3 Resource Allocation Graph

```
    hacker_0 ─────> hackerQueue (bekliyor)
         ↑               │
         │               │
         └───────────────┘ (release bekleniyor)

    hacker_1 ─────> hackerQueue (bekliyor)
         ↑               │
         │               │
         └───────────────┘ (release bekleniyor)

    serf___0 ─────> serfQueue (bekliyor)
         ↑               │
         │               │
         └───────────────┘ (release bekleniyor)
```

**Analiz**:
- Hiçbir thread release yapmıyor çünkü hiçbir koşul sağlanmadı
- Circular dependency yok ama **deadlock var**
- Çünkü: Kaynağı serbest bırakacak olan (kaptan) hiçbir zaman seçilemiyor

### 6.4 Deadlock Tespiti (DeadlockDetector)

**Algoritma**:
```java
boolean isDeadlocked(StateSnapshot state) {
    // 1. Durum değişikliğinden beri geçen süre
    long timeSinceChange = now() - lastStateChange;
    
    // 2. Thread'ler bekliyor mu?
    boolean threadsWaiting = state.totalWaiting() > 0;
    
    // 3. Tekne oluşturulabilir mi?
    boolean canFormBoat = (state.hackers >= 4) ||
                         (state.serfs >= 4) ||
                         (state.hackers >= 2 && state.serfs >= 2);
    
    // DEADLOCK = Uzun süre değişiklik yok + Bekleyenler var + Tekne oluşturulamıyor
    return (timeSinceChange > 3000) && threadsWaiting && !canFormBoat;
}
```

**Tespit Sonrası Aksiyon**:
1. Detaylı analiz yazdır
2. Bekleyen thread'leri listele
3. Neden deadlock olduğunu açıkla
4. Çözüm öner

---

## 7. Starvation Analizi

### 7.1 Starvation Nedir?

**Tanım**: Bir thread'in çok uzun süre CPU zamanı alamaması veya kaynağa erişememesi.

**River Crossing'de Starvation**:
- Bir tip thread (hacker veya serf) sürekli geçiyor
- Diğer tip thread uzun süre bekliyor

### 7.2 Starvation Senaryosu (10H + 2S)

**Başlangıç**: 10 hacker, 2 serf

**Beklenen Akış**:
```
Sefer 1: 4 Hacker → hacker_0, hacker_1, hacker_2, hacker_3
Sefer 2: 4 Hacker → hacker_4, hacker_5, hacker_6, hacker_7
Sefer 3: 2 Hacker + 2 Serf → hacker_8, hacker_9, serf___0, serf___1
```

**Starvation Gösterimi**:
- Serf'ler ilk 2 seferde hiç geçemiyor
- 2000+ ms bekliyorlar (starvation!)
- Hackerlar hızlı geçiyor (0-50ms bekleme)

**Loglama**:
```
11:25:02.456 [WARNING] serf___0 ⚠️ STARVATION: 2300ms gibi uzun bir süre bekledi!
11:25:02.456 [WARNING] serf___1 ⚠️ STARVATION: 2299ms gibi uzun bir süre bekledi!
```

### 7.3 Unfairness Nedenleri

1. **Random Timing**: Thread'ler rastgele zamanlarda geliyorlar
2. **Homojen Gruplar Öncelikli**: 4H veya 4S koşulu önce kontrol ediliyor
3. **İlk Gelen Önce**: FIFO sırası yok, kim mutex'i alırsa o kontrol ediyor

**Matematiksel Analiz**:
- 10H, 2S durumunda hackerların tekne oluşturma olasılığı yüksek
- İlk 8 hacker geldiğinde: 2 sefer yapabilirler (4H + 4H)
- Serfler ancak hackerlar bittikten sonra şanslarını bulurlar

---

## 8. Race Condition'lar

### 8.1 Korunan Bölgeler (Mutex ile)

**Güvenli Kod**:
```java
smphrs.mutex.acquire();
smphrs.hackers++;  // ✅ Atomik güncelleme
if (smphrs.hackers == 4) { ... }
smphrs.mutex.release();
```

### 8.2 Potansiyel Race Condition (Mevcut Kodda YOK)

**Eğer mutex olmasaydı**:
```java
// Thread A
int h = smphrs.hackers;  // Oku: 3
h++;                     // Hesapla: 4
smphrs.hackers = h;      // Yaz: 4

// Thread B (aynı anda)
int h = smphrs.hackers;  // Oku: 3 (Thread A yazmadan önce)
h++;                     // Hesapla: 4
smphrs.hackers = h;      // Yaz: 4

// Sonuç: hackers = 4 (olması gereken: 5!)
```

### 8.3 Memory Visibility (Volatile Kullanımı)

**lastStateChange** volatile:
```java
private volatile long lastStateChange;
```

**Neden?**:
- DeadlockDetector başka bir thread'de çalışıyor
- Değişiklikleri hemen görmesi gerekiyor
- Volatile keyword memory barrier sağlıyor

**Olmadan Ne Olurdu?**:
- CPU cache'de eski değer kalabilirdi
- Deadlock tespiti gecikebilirdi

---

## 9. Çözümler ve İyileştirmeler

### 9.1 Timeout Mekanizması

**Amaç**: Sonsuz beklemeyi önlemek

**İmplementasyon**:
```java
boolean success = smphrs.hackerQueue.tryAcquire(
    config.timeoutMs,  // 5000ms
    TimeUnit.MILLISECONDS
);

if (!success) {
    Logger.error("TIMEOUT - Deadlock olabilir!");
    return;  // Thread sonlanıyor (graceful exit)
}
```

**Avantajlar**:
- Deadlock'ta program sonsuza kadar takılmıyor
- Graceful degradation sağlanıyor
- Kullanıcıya bilgi veriliyor

### 9.2 CyclicBarrier Kullanımı

**Problem**: Mevcut kod counting semaphore kullanıyor

**Çözüm**:
```java
CyclicBarrier barrier = new CyclicBarrier(4, () -> {
    Logger.info("Tüm yolcular tekneye bindi!");
});

// Her thread
barrier.await();  // 4 kişi toplanana kadar bekle

// Otomatik reset - sonraki grup için hazır
```

**Avantajlar**:
- Otomatik reset
- Barrier action özelliği
- Thread-safe
- Daha güvenli

### 9.3 Fairness Queue İmplementasyonu

**Amaç**: FIFO sırasını garanti etmek

**İmplementasyon**:
```java
// Semaphors.java
private Queue<String> hackerWaitQueue = new LinkedList<>();

public synchronized void enqueueHacker(String name) {
    hackerWaitQueue.offer(name);
}

// Release yaparken sırayla
String next = hackerWaitQueue.poll();
// next thread'i uyandır
```

### 9.4 Deadlock Önleme Stratejileri

**Strateji 1: Thread Sayısını Kontrol Et**
```java
// App başlangıcında
if ((numHackers + numSerfs) % 4 != 0) {
    Logger.warning("Uyarı: Toplam kişi sayısı 4'ün katı değil, bazıları geçemeyebilir");
}
```

**Strateji 2: Dinamik Thread Üretimi**
```java
// Eğer deadlock tespit edilirse
if (cannotFormBoat() && moreThreadsNeeded()) {
    createAdditionalThreads();
}
```

**Strateji 3: Resource Ordering**
- Her zaman aynı sırada kaynak al
- Circular wait'i önler

---

## 10. Teorik Arka Plan

### 10.1 Semaphore Teorisi (Dijkstra)

**Tanım**: Edsger Dijkstra tarafından 1965'te önerilen senkronizasyon primitive'i.

**Operasyonlar**:
- **P (Proberen/Test)**: `acquire()` - permit azalt, 0 ise bekle
- **V (Verhogen/Increment)**: `release()` - permit arttır

**Matematiksel Özellikler**:
```
Invariant: permits ≥ 0
P(s): if (s > 0) s-- else wait
V(s): s++; wakeup_one_waiter()
```

**Kullanım Alanları**:
1. Mutual Exclusion (Binary Semaphore, n=1)
2. Resource Counting (Counting Semaphore, n>1)
3. Signaling (Başlangıç n=0)

### 10.2 Monitor Pattern

**Tanım**: Higher-level senkronizasyon abstraction'ı.

**Java'da**:
```java
synchronized void criticalSection() {
    // Otomatik mutex
    // ...
    wait();    // Condition variable
    notify();  // Wake up
}
```

**Semaphore vs Monitor**:
| Özellik | Semaphore | Monitor |
|---------|-----------|---------|
| Level | Low-level | High-level |
| Hata Olasılığı | Yüksek | Düşük |
| Flexibility | Yüksek | Orta |
| Java Support | java.util.concurrent | synchronized keyword |

### 10.3 Barrier Senkronizasyonu

**Tanım**: N thread'in bir noktada toplanmasını sağlayan mechanism.

**Türler**:
1. **Counting Barrier** (mevcut implementasyon)
2. **CyclicBarrier** (Java sağlar)
3. **Phaser** (Java 7+, daha gelişmiş)

**CyclicBarrier Özellikleri**:
```java
CyclicBarrier barrier = new CyclicBarrier(parties, barrierAction);

barrier.await();  // Block until parties arrive
// Otomatik reset, tekrar kullanılabilir
```

### 10.4 Dining Philosophers Benzerliği

**Problem**: 5 filozof, 5 çatal, yemek yeme.

**River Crossing ile Karşılaştırma**:
| Özellik | Dining Philosophers | River Crossing |
|---------|---------------------|----------------|
| Kaynaklar | Çatallar | Tekne slotları |
| Thread'ler | Filozoflar | Hackers/Serfs |
| Deadlock | Hepsi sol çatalı alırsa | 2H+1S durumu |
| Çözüm | Resource ordering | Timeout / Balanced input |

### 10.5 Producer-Consumer Benzerliği

**Producer-Consumer Pattern**:
```
Producer → [Buffer] → Consumer
```

**River Crossing Benzeri**:
```
Arriving Threads → [Waiting Area] → Boat (Consumer)
```

**Ortak Özellikler**:
- Bounded buffer (Tekne kapasitesi = 4)
- Blocking queue (hackerQueue, serfQueue)
- Synchronization (mutex, barrier)

### 10.6 Happens-Before İlişkileri

**Java Memory Model**:
- `mutex.acquire()` happens-before her şey kritik bölgede
- Kritik bölgedeki her şey happens-before `mutex.release()`
- `release()` happens-before sonraki `acquire()`

**Garanti**:
```
Thread A:
  mutex.acquire()
  hackers++  // (1)
  mutex.release()

Thread B:
  mutex.acquire()
  read(hackers)  // (2) - Mutlaka güncel değeri görür
  mutex.release()
```

(1) happens-before (2) çünkü mutex sıralama garantisi veriyor.

---

## 11. Programı Çalıştırma

### 11.1 Derleme

```bash
javac *.java
```

### 11.2 Mod Seçimi

```bash
# BALANCED - Dengeli senaryo
java App BALANCED

# DEADLOCK - Deadlock gösterimi
java App DEADLOCK

# STARVATION - Açlık gösterimi
java App STARVATION

# FIXED - Düzeltilmiş versiyon
java App FIXED

# Argümansız (varsayılan: BALANCED)
java App
```

### 11.3 Beklenen Çıktılar

#### BALANCED Modu
```
🚀 PROGRAM BAŞLADI
hacker_0 geldi. Bekleyen: H=1, S=0
...
hacker_3 🎯 KAPTAN OLDU! 4 hacker bulundu
🚣 TEKNE KALKIYOR!
✅ Geçiş #1 tamamlandı!
...
Durum: ✅ BAŞARILI - Tüm kişiler karşıya geçti!
```

#### DEADLOCK Modu
```
🔍 Deadlock Detector başlatıldı
hacker_0 ⏳ bekliyor (H=1, S=0)
hacker_1 ⏳ bekliyor (H=2, S=0)
serf___0 ⏳ bekliyor (H=2, S=1)

🚨 DEADLOCK TESPİT EDİLDİ! 🚨
Mevcut durum: 2 hacker + 1 serf
❌ Hiçbir geçerli kompozisyon sağlanamıyor!
💡 2H+1S durumu: 1 serf daha gelmeli ama gelmeyecek

⚠️ TIMEOUT! hackerQueue'da çok uzun bekledi!
Durum: ❌ DEADLOCK - Sistem kilitlendi!
```

---

## 12. İleri Seviye Konular

### 12.1 Performans Analizi

**Metrikler**:
- **Throughput**: Birim zamanda kaç kişi geçiyor?
- **Latency**: Bir kişi ne kadar bekliyor?
- **Utilization**: Tekne ne kadar verimli kullanılıyor?

**BALANCED Modu Analizi** (8H + 8S):
```
Total Time: ~4 seconds (4 seferx1s)
Throughput: 16 people / 4s = 4 people/s
Average Latency: ~2s (ortalama bekleme)
Boat Utilization: 100% (her sefer tam dolu)
```

### 12.2 Ölçeklenebilirlik

**Soru**: 1000 hacker + 1000 serf için ne olur?

**Cevap**:
- 500 sefer gerekir
- ~500 saniye (8.3 dakika)
- Memory: O(n) thread'ler için
- CPU: Her thread az CPU kullanıyor (çoğu zaman blocked)

**Optimizasyon**:
- Thread pool kullanımı
- Batch processing (10'luk gruplar?)
- Parallel boats (çoklu tekne)

### 12.3 Alternatif İmplementasyonlar

**1. Monitor-Based**:
```java
class BoatMonitor {
    synchronized void board() {
        while (!canBoard()) {
            wait();
        }
        // Board logic
        notifyAll();
    }
}
```

**Avantajlar**:
- Daha yüksek seviye
- Daha az hata
- synchronized keyword kullanımı

**Dezavantajlar**:
- Daha az esneklik
- Semaphore'dan daha az kontrol

**2. Lock-Based** (ReentrantLock):
```java
Lock lock = new ReentrantLock();
Condition hackerCondition = lock.newCondition();
Condition serfCondition = lock.newCondition();

lock.lock();
try {
    while (!canBoard()) {
        hackerCondition.await();
    }
} finally {
    lock.unlock();
}
```

**Avantajlar**:
- Trylock desteği
- Multiple conditions
- Fairness desteği (fair lock)

**3. CompletableFuture-Based** (Modern Java):
```java
CompletableFuture<Void> boarding = CompletableFuture
    .runAsync(() -> board())
    .thenRun(() -> rowBoat())
    .exceptionally(ex -> handleError(ex));
```

### 12.4 Dağıtık Sistem Uygulamaları

**Soru**: Birden fazla sunucuda river crossing?

**Çözüm Gereksinimleri**:
1. **Distributed Lock**: Zookeeper, Redis
2. **Message Queue**: RabbitMQ, Kafka
3. **Consensus**: Raft, Paxos

**Mimari**:
```
┌─────────────┐
│  Server 1   │ ──┐
└─────────────┘   │
                  ├──> ┌──────────────┐
┌─────────────┐   │    │   Zookeeper  │ (Coordinator)
│  Server 2   │ ──┤    └──────────────┘
└─────────────┘   │
                  │
┌─────────────┐   │
│  Server 3   │ ──┘
└─────────────┘
```

**Challenges**:
- Network partitions
- Clock synchronization
- Failure detection

### 12.5 Real-World Uygulamalar

**1. Thread Pool Management**:
- Worker threads için slot yönetimi
- Resource pooling (DB connections)

**2. Load Balancing**:
- Request batching
- Group formation

**3. Workflow Orchestration**:
- Task coordination
- Barrier synchronization for parallel stages

**4. Gaming**:
- Matchmaking (4 kişilik takımlar)
- Lobby systems

---

## Sonuç

Bu implementasyon, River Crossing Problem'in kapsamlı bir çözümünü sunmaktadır. Temel senkronizasyon kavramlarından (mutex, semaphore, barrier) ileri seviye konulara (deadlock detection, timeout, graceful degradation) kadar geniş bir yelpazede öğrenme fırsatı sağlamaktadır.

**Öğrenilen Kavramlar**:
✅ Semaphore kullanımı
✅ Mutual exclusion
✅ Barrier synchronization
✅ Deadlock detection ve analizi
✅ Starvation scenarios
✅ Timeout mekanizmaları
✅ Thread coordination
✅ State management

**Sunum için Öneriler**:
1. BALANCED modu ile başlayın (başarılı senaryo)
2. DEADLOCK modunu gösterin (problem gösterimi)
3. DeadlockDetector'ın analizini vurgulayın
4. STARVATION ile unfairness gösterin
5. FIXED modu ile çözümü sunun
6. Kod detaylarına dalın (Hacker.java, Semaphors.java)
7. Teorik arka planı açıklayın

---

## 13. Detaylı Örnek Senaryo - Kod Satır Satır İzleme

Bu bölümde **4 Hacker'ın geçişi** senaryosunu adım adım inceleyeceğiz. Her adımda hangi dosyanın hangi satırının çalıştığını göstereceğiz.

### 13.1 Senaryo: 4 Hacker Geçişi

**Başlangıç Durumu**:
- 4 hacker thread başlatılacak: `hacker_0`, `hacker_1`, `hacker_2`, `hacker_3`
- Beklenen sonuç: 4 hacker bir tekneye binip karşıya geçecek

### 13.2 Zaman Çizelgesi ve Kod Akışı

---

#### ⏰ t=0ms - Program Başlangıcı

**App.java:2** - `main()` metodu başlıyor
```java
public static void main(String[] args) throws InterruptedException {
```

**App.java:5** - Banner yazdırılıyor
```java
printBanner();
```

**App.java:8** - Config oluşturuluyor (4 hacker için özel config)
```java
Config config = parseArguments(args);
```

**Config.java:32-38** - BALANCED modu için config hazırlanıyor
```java
return new Config(
    Mode.BALANCED,
    4,  // numHackers = 4
    0,  // numSerfs = 0 (basit örnek için)
    false, false, 0, LogLevel.INFO
);
```

**App.java:25** - Semaphors nesnesi yaratılıyor
```java
Semaphors smphrs = new Semaphors(config);
```

**Semaphors.java:19-26** - Semaforlar başlatılıyor
```java
public Semaphors(Config config) {
    this.config = config;
    this.mutex = new Semaphore(1);      // 1 permit - mutual exclusion
    this.hackerQueue = new Semaphore(0); // 0 permit - blocking queue
    this.serfQueue = new Semaphore(0);   // 0 permit - blocking queue
    this.barrier = new Semaphore(4);     // 4 permit - barrier sync
}
```

**App.java:38-41** - Hacker thread'leri oluşturuluyor
```java
for (int i = 0; i < hackers.length; i++) {
    thrdHackers[i] = new Thread(new Hacker(("hacker_" + i), smphrs));
    thrdHackers[i].setName("hacker_" + i);
}
```

**App.java:52-53** - Thread'ler başlatılıyor
```java
if (i < hackers.length)
    thrdHackers[i].start();
```

---

#### ⏰ t=5ms - hacker_0 Çalışmaya Başlıyor

**Hacker.java:17** - `run()` metodu başlıyor
```java
public void run() {
```

**Hacker.java:18** - Thread başlangıç logu
```java
Logger.debug(ad + " thread başladı");
```
**Çıktı**: `[DEBUG] hacker_0 thread başladı`

**Hacker.java:23** - Mutex kilitlenmesi için bekleme başlıyor
```java
smphrs.mutex.acquire();  // ✅ İLK GELİYOR, HEMEN ALIR
```

**Hacker.java:26** - Waiting list'e ekleniyor
```java
smphrs.addWaitingHacker(ad);
```

**Semaphors.java:74-76** - Thread bilgisi kaydediliyor
```java
public void addWaitingHacker(String name) {
    waitingHackers.add(new ThreadInfo(name));
}
```

**Hacker.java:28-29** - Sayaç artırılıyor
```java
smphrs.hackers++;
smphrs.updateState();  // lastStateChange güncelleniyor
```

**Semaphors.java:106** - State güncellemesi
```java
public void updateState() {
    this.lastStateChange = System.currentTimeMillis();
    // State history'ye ekle
}
```

**Hacker.java:30** - Log mesajı
```java
Logger.info(ad + " geldi. Bekleyen: H=" + smphrs.hackers + ", S=" + smphrs.serfs);
```
**Çıktı**: `[INFO] [hacker_0] hacker_0 geldi. Bekleyen: H=1, S=0`

**Hacker.java:34-35** - Koşul kontrolü
```java
if (smphrs.hackers == 4) {  // ❌ 1 == 4? FALSE
    // Çalışmaz
```

**Hacker.java:46-47** - İkinci koşul kontrolü
```java
} else if (smphrs.hackers == 2 && smphrs.serfs >= 2) {  // ❌ FALSE
    // Çalışmaz
```

**Hacker.java:60** - Her iki koşul sağlanmadı, log
```java
Logger.debug(ad + " ⏳ bekliyor (H=" + smphrs.hackers + ", S=" + smphrs.serfs + ")");
```
**Çıktı**: `[DEBUG] [hacker_0] hacker_0 ⏳ bekliyor (H=1, S=0)`

**Hacker.java:63** - Mutex serbest bırakılıyor
```java
smphrs.mutex.release();
```

**Hacker.java:67-75** - hackerQueue'da beklemeye geçiyor
```java
if (config.enableTimeout) {
    // Timeout yok bu modda
} else {
    smphrs.hackerQueue.acquire();  // 🔒 BLOCKING! KUYRUKTA BEKLİYOR
}
```

> **DURUM**: `hacker_0` artık `hackerQueue.acquire()` satırında **BLOKLU** durumda.
> Birisi `hackerQueue.release()` yapana kadar bekleyecek.

---

#### ⏰ t=7ms - hacker_1 Çalışmaya Başlıyor

**Aynı akış tekrar ediyor, ancak farklar**:

**Hacker.java:23** - Mutex kilitlenmesi
```java
smphrs.mutex.acquire();  // ✅ hacker_0 release yaptı, alabilir
```

**Hacker.java:28-29** - Sayaç artırılıyor
```java
smphrs.hackers++;  // hackers şimdi 2
```

**Hacker.java:30** - Log
```java
Logger.info(ad + " geldi. Bekleyen: H=" + smphrs.hackers + ", S=" + smphrs.serfs);
```
**Çıktı**: `[INFO] [hacker_1] hacker_1 geldi. Bekleyen: H=2, S=0`

**Hacker.java:34** - Koşul kontrolü
```java
if (smphrs.hackers == 4) {  // ❌ 2 == 4? FALSE
```

**Hacker.java:46** - İkinci koşul
```java
} else if (smphrs.hackers == 2 && smphrs.serfs >= 2) {  // ❌ serfs=0, FALSE
```

**Hacker.java:67-75** - Kuyrukta beklemeye geçiyor
```java
smphrs.hackerQueue.acquire();  // 🔒 BLOCKING!
```

> **DURUM**: Şimdi `hacker_0` ve `hacker_1` ikisi de `hackerQueue`'da bloklu.

---

#### ⏰ t=9ms - hacker_2 Çalışmaya Başlıyor

**Aynı akış**:

**Hacker.java:28-29** - Sayaç artırılıyor
```java
smphrs.hackers++;  // hackers şimdi 3
```

**Hacker.java:30** - Log
```java
Logger.info(ad + " geldi. Bekleyen: H=" + smphrs.hackers + ", S=" + smphrs.serfs);
```
**Çıktı**: `[INFO] [hacker_2] hacker_2 geldi. Bekleyen: H=3, S=0`

**Hacker.java:34** - Koşul kontrolü
```java
if (smphrs.hackers == 4) {  // ❌ 3 == 4? FALSE
```

**Hacker.java:67-75** - Kuyrukta beklemeye geçiyor
```java
smphrs.hackerQueue.acquire();  // 🔒 BLOCKING!
```

> **DURUM**: `hacker_0`, `hacker_1`, `hacker_2` üçü de `hackerQueue`'da bloklu.

---

#### ⏰ t=11ms - hacker_3 Çalışmaya Başlıyor (KRİTİK NOKTA!)

**Hacker.java:23** - Mutex alınıyor
```java
smphrs.mutex.acquire();  // ✅ ALIYOR
```

**Hacker.java:28-29** - Sayaç artırılıyor
```java
smphrs.hackers++;  // hackers şimdi 4 ✅
```

**Hacker.java:30** - Log
```java
Logger.info(ad + " geldi. Bekleyen: H=" + smphrs.hackers + ", S=" + smphrs.serfs);
```
**Çıktı**: `[INFO] [hacker_3] hacker_3 geldi. Bekleyen: H=4, S=0`

**Hacker.java:34** - Koşul kontrolü
```java
if (smphrs.hackers == 4) {  // ✅ 4 == 4? TRUE!
```

**Hacker.java:35** - Kaptan oluyor!
```java
isCaptain = true;
```

**Hacker.java:36** - Log
```java
Logger.info(ad + " 🎯 KAPTAN OLDU! 4 hacker bulundu");
```
**Çıktı**: `[INFO] [hacker_3] hacker_3 🎯 KAPTAN OLDU! 4 hacker bulundu`

**Hacker.java:37-38** - hackerQueue'yu serbest bırakıyor (4 thread uyandırılıyor!)
```java
smphrs.hackerQueue.release(4);
smphrs.hackers = 0;  // Sayacı sıfırlıyor
```

> **ÖNEMLİ**: Bu satır çalıştığında:
> - `hacker_0`, `hacker_1`, `hacker_2` **UYANIYOR** 🔓
> - Onlar da `hackerQueue.acquire()` satırından devam ediyorlar!

**Hacker.java:63** - Mutex release (artık kritik bölgeden çıktı)
```java
smphrs.mutex.release();
```

---

#### ⏰ t=12ms - 4 Hacker Tekneye Biniyor

Artık 4 thread de (`hacker_0`, `hacker_1`, `hacker_2`, `hacker_3`) paralel çalışıyor.

**Hacker.java:79** - Waiting list'ten çıkarılıyor
```java
smphrs.removeWaitingHacker(ad);
```

**Semaphors.java:80-86** - Thread kaydı siliniyor
```java
public void removeWaitingHacker(String name) {
    waitingHackers.removeIf(info -> info.name.equals(name));
}
```

**Hacker.java:80** - Bekleme süresi hesaplanıyor
```java
long waitTime = System.currentTimeMillis() - startTime;
```

**Hacker.java:81-83** - Log
```java
Logger.debug(ad + " tekneye bindi (bekleme süresi: " + waitTime + "ms)");
```
**Çıktılar** (paralel):
```
[DEBUG] [hacker_0] hacker_0 tekneye bindi (bekleme süresi: 7ms)
[DEBUG] [hacker_1] hacker_1 tekneye bindi (bekleme süresi: 5ms)
[DEBUG] [hacker_2] hacker_2 tekneye bindi (bekleme süresi: 3ms)
[DEBUG] [hacker_3] hacker_3 tekneye bindi (bekleme süresi: 1ms)
```

**Hacker.java:86** - Barrier'da bekleme başlıyor
```java
smphrs.barrier.acquire();  // 4 kişi acquire yapana kadar bekle
```

**Semaphors.java:19-26** - Barrier başlangıçta 4 permit ile yaratılmıştı
```java
this.barrier = new Semaphore(4);
```

Her thread `barrier.acquire()` yaptığında permit azalıyor:
- `hacker_0` acquire → permits: 3
- `hacker_1` acquire → permits: 2
- `hacker_2` acquire → permits: 1
- `hacker_3` acquire → permits: 0

Hepsi geçiyor (4 permit vardı), şimdi barrier boş.

---

#### ⏰ t=13ms - Kaptan Tekneyi Hareket Ettiriyor

**Hacker.java:88** - Kaptan kontrolü
```java
if (isCaptain) {  // ✅ Sadece hacker_3 için TRUE
```

**Hacker.java:89** - rowBoat() çağrısı
```java
rowBoat();
```

**Person.java:14** - rowBoat() metodu
```java
protected void rowBoat() {
```

**Person.java:15** - Log
```java
Logger.info("🚣 TEKNE KALKIYOR!");
```
**Çıktı**: `[INFO] [hacker_3] 🚣 TEKNE KALKIYOR!`

**Person.java:16** - Kaptan bilgisi
```java
Logger.info("Kaptan: " + ad);
```
**Çıktı**: `[INFO] [hacker_3] Kaptan: hacker_3`

**Person.java:19** - 1 saniye bekleme (geçişi simüle ediyor)
```java
Thread.sleep(1000);
```

> **NOT**: Bu süre zarfında diğer thread'ler (`hacker_0`, `hacker_1`, `hacker_2`)
> `barrier.acquire()` satırından sonra bekliyor. Onlar kaptan olmadığı için `else` bloğuna gidecekler.

---

#### ⏰ t=1013ms - Geçiş Tamamlandı, Barrier Release

**Person.java:23** - Geçiş sayacı artırılıyor
```java
smphrs.incrementCrossingCount();
```

**Semaphors.java:148** - Atomic increment
```java
public void incrementCrossingCount() {
    crossingCount.incrementAndGet();
}
```

**Person.java:24** - Log
```java
Logger.info("✅ Geçiş #" + smphrs.getCrossingCount() + " tamamlandı!");
```
**Çıktı**: `[INFO] [hacker_3] ✅ Geçiş #1 tamamlandı!`

**Hacker.java:90** - Barrier release (diğer 3 thread'i uyandırıyor!)
```java
smphrs.barrier.release(4);  // 4 permit geri veriliyor
```

> **ÖNEMLİ**: Ama diğer thread'ler zaten barrier'dan geçtiler (çünkü başlangıçta 4 permit vardı).
> Bu release, **sonraki tekne** için barrier'ı hazırlıyor!

**Hacker.java:91** - Mutex release
```java
smphrs.mutex.release();
```

**Hacker.java:93-95** - else bloğu (diğer 3 thread için)
```java
} else {
    // Kaptan değiliz, sadece bekle
    Logger.debug(ad + " yolcu olarak geçti");
}
```
**Çıktılar**:
```
[DEBUG] [hacker_0] hacker_0 yolcu olarak geçti
[DEBUG] [hacker_1] hacker_1 yolcu olarak geçti
[DEBUG] [hacker_2] hacker_2 yolcu olarak geçti
```

**Hacker.java:97** - run() metodu bitiyor
```java
Logger.debug(ad + " thread sonlandı");
```
**Çıktılar**:
```
[DEBUG] [hacker_0] hacker_0 thread sonlandı
[DEBUG] [hacker_1] hacker_1 thread sonlandı
[DEBUG] [hacker_2] hacker_2 thread sonlandı
[DEBUG] [hacker_3] hacker_3 thread sonlandı
```

---

#### ⏰ t=1015ms - Program Sonlanıyor

**App.java:77-89** - Thread join bekleme
```java
if (i < hackers.length) {
    thrdHackers[i].join(remaining);  // ✅ Thread'ler bitti, hemen dönüyor
```

**App.java:98** - Özet yazdırma
```java
printExecutionSummary(allCompleted, smphrs, detector, config);
```

**App.java:151-157** - İstatistikler
```java
int totalCrossings = smphrs.getCrossingCount();  // 1
int expectedCrossings = (config.numHackers + config.numSerfs) / 4;  // 4/4=1
```

**App.java:159-160** - Başarı mesajı
```java
if (allCompleted && totalCrossings == expectedCrossings) {
    Logger.info("Durum: ✅ BAŞARILI - Tüm kişiler karşıya geçti!");
}
```
**Çıktı**: `[INFO] Durum: ✅ BAŞARILI - Tüm kişiler karşıya geçti!`

---

### 13.3 Senaryo Özeti: Kod Akış Tablosu

| Zaman | Thread | Dosya:Satır | Aksiyon | Sonuç |
|-------|--------|-------------|---------|-------|
| t=0 | main | App.java:2 | Program başlat | - |
| t=0 | main | App.java:25 | Semaphors oluştur | mutex=1, queues=0, barrier=4 |
| t=5 | hacker_0 | Hacker.java:23 | mutex.acquire() | ✅ Aldı |
| t=5 | hacker_0 | Hacker.java:29 | hackers++ | hackers=1 |
| t=5 | hacker_0 | Hacker.java:34 | if(hackers==4) | ❌ FALSE |
| t=5 | hacker_0 | Hacker.java:63 | mutex.release() | Mutex serbest |
| t=5 | hacker_0 | Hacker.java:74 | hackerQueue.acquire() | 🔒 BLOCKED |
| t=7 | hacker_1 | Hacker.java:29 | hackers++ | hackers=2 |
| t=7 | hacker_1 | Hacker.java:74 | hackerQueue.acquire() | 🔒 BLOCKED |
| t=9 | hacker_2 | Hacker.java:29 | hackers++ | hackers=3 |
| t=9 | hacker_2 | Hacker.java:74 | hackerQueue.acquire() | 🔒 BLOCKED |
| t=11 | hacker_3 | Hacker.java:29 | hackers++ | hackers=4 |
| t=11 | hacker_3 | Hacker.java:34 | if(hackers==4) | ✅ TRUE! |
| t=11 | hacker_3 | Hacker.java:35 | isCaptain=true | Kaptan seçildi |
| t=11 | hacker_3 | Hacker.java:37 | hackerQueue.release(4) | 4 thread uyandı! 🔓 |
| t=12 | hacker_0-3 | Hacker.java:79 | removeWaitingHacker() | Liste temizleme |
| t=12 | hacker_0-3 | Hacker.java:86 | barrier.acquire() | ✅ Geçtiler (4 permit vardı) |
| t=13 | hacker_3 | Person.java:15 | rowBoat() | Tekne kalktı! |
| t=1013 | hacker_3 | Person.java:23 | incrementCrossingCount() | crossingCount=1 |
| t=1013 | hacker_3 | Hacker.java:90 | barrier.release(4) | Barrier reset |
| t=1015 | main | App.java:159 | Özet yazdır | ✅ BAŞARILI |

---

### 13.4 Kritik Satırlar ve Rolleri

#### 🔐 Mutex Koruması
```java
// Hacker.java:23
smphrs.mutex.acquire();  // Kritik bölgeye giriş - Race condition önleme

// Hacker.java:29
smphrs.hackers++;  // ✅ Atomik güncelleme (mutex korumasında)

// Hacker.java:63
smphrs.mutex.release();  // Kritik bölgeden çıkış
```

**Rol**: `hackers` sayacının aynı anda iki thread tarafından güncellenmesini önler.

---

#### 🚦 Queue Senkronizasyonu
```java
// Hacker.java:37 (Kaptan)
smphrs.hackerQueue.release(4);  // 4 thread'i uyandırma

// Hacker.java:74 (Yolcular)
smphrs.hackerQueue.acquire();  // Uyandırılmayı bekleme (BLOCKING)
```

**Rol**: Thread'leri bekletip gerektiğinde uyandırma mekanizması.

---

#### 🚧 Barrier Senkronizasyonu
```java
// Hacker.java:86 (Hepsi)
smphrs.barrier.acquire();  // 4 kişi toplanma noktası

// Hacker.java:90 (Kaptan)
smphrs.barrier.release(4);  // Sonraki grup için reset
```

**Rol**: 4 kişinin birlikte hareket etmesini sağlama.

---

#### 🎯 Kaptan Seçimi
```java
// Hacker.java:34-35
if (smphrs.hackers == 4) {  // Koşul sağlandı mı?
    isCaptain = true;  // Bu thread kaptan oldu!

// Hacker.java:88-91
if (isCaptain) {
    rowBoat();  // Sadece kaptan tekneyi hareket ettirir
    smphrs.barrier.release(4);  // Sadece kaptan barrier'ı reset eder
}
```

**Rol**: Koşulu sağlayan thread "kaptan" olarak seçiliyor ve özel görevleri yerine getiriyor.

---

### 13.5 Alternatif Senaryo: Deadlock (2H + 1S)

#### Başlangıç
```
Thread'ler: hacker_0, hacker_1, serf___0
```

#### Kritik Fark

**t=11ms - serf___0 geliyor**:

**Serf.java:29** - Sayaç artırılıyor
```java
smphrs.serfs++;  // serfs=1
```

**Serf.java:34-37** - Koşul kontrolleri
```java
if (smphrs.serfs == 4) {  // ❌ 1 == 4? FALSE

} else if (smphrs.hackers >= 2 && smphrs.serfs == 2) {  // ❌ serfs=1, FALSE
```

**Durum**:
```
hackers = 2 (hacker_0, hacker_1 queue'da bekliyor)
serfs = 1 (serf___0 queue'da bekliyor)
```

**Hiçbir koşul sağlanmıyor**:
- ❌ `hackers == 4` (sadece 2 var)
- ❌ `serfs == 4` (sadece 1 var)
- ❌ `hackers == 2 && serfs >= 2` (serfs sadece 1)

**Serf.java:74** - Queue'da bloklaniyor
```java
smphrs.serfQueue.acquire();  // 🔒 SONSUZ BEKLEYECEK!
```

---

#### Deadlock Tespiti

**DeadlockDetector.java:30** - Her 1 saniyede kontrol
```java
Thread.sleep(1000);
```

**DeadlockDetector.java:32** - State snapshot alınıyor
```java
StateSnapshot currentState = semaphors.getCurrentState();
```

**DeadlockDetector.java:35** - Deadlock kontrolü
```java
if (isDeadlocked(currentState)) {
```

**DeadlockDetector.java:58-74** - isDeadlocked() metodu
```java
long timeSinceChange = System.currentTimeMillis() - semaphors.getLastStateChange();
boolean threadsWaiting = state.totalWaiting() > 0;  // ✅ 3 thread bekliyor
boolean canFormBoat = state.canFormBoat();  // ❌ FALSE (2H+1S geçersiz)

return (timeSinceChange > 3000) && threadsWaiting && !canFormBoat;  // ✅ TRUE!
```

**DeadlockDetector.java:41** - Deadlock mesajı
```java
Logger.error("🚨 DEADLOCK TESPİT EDİLDİ! 🚨");
```

---

### 13.6 Öğrenme Çıkarımları

Bu detaylı izleme size şunları göstermiştir:

1. **Mutex'in Rolü**: Her thread `hackers++` yapmadan önce mutex alıyor, race condition önleniyor.

2. **Queue Mekanizması**: `hackerQueue.acquire()` thread'i blokluyor, `release(4)` ile uyandırılıyorlar.

3. **Barrier Kullanımı**: 4 kişi `barrier.acquire()` yapıyor, sonra kaptan `release(4)` ile reset ediyor.

4. **Kaptan Seçimi**: Koşulu sağlayan thread otomatik olarak kaptan oluyor ve rowBoat() çağırıyor.

5. **Deadlock Tespiti**: DeadlockDetector arka planda çalışıp durum değişikliklerini izliyor.

6. **State Management**: Her değişiklikte `updateState()` çağrılıyor, timestamp güncelleniyor.

Bu bilgiyi kullanarak kodun nasıl çalıştığını sunumunuzda etkili bir şekilde anlatabilirsiniz!