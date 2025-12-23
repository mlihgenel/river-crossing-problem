# River Crossing Problem - Hacker & Serf Synchronization

Bu proje, klasik **River Crossing Problem** çözümünün kapsamlı bir demonstrasyonudur. Hacker'lar ve Serf'lerin bir nehri geçmek için senkronize olmasını sağlar.

## 🚀 Hızlı Başlangıç

### Derleme

```bash
javac *.java
```

### Çalıştırma

```bash
# Dengeli senaryo (8 hacker + 8 serf)
java App BALANCED

# Deadlock gösterimi (2 hacker + 1 serf)
java App DEADLOCK

# Starvation gösterimi (10 hacker + 2 serf)
java App STARVATION

# Düzeltilmiş versiyon (timeout ile)
java App FIXED
```

## 📋 Problem Tanımı

### Tekne Özellikleri
- **Kapasite**: Tam 4 kişi
- **Geçiş**: Her sefer sonra tekne otomatik geri dönüyor
- **Örnek**: 16 kişi için 4 sefer gerekli (16 ÷ 4 = 4)

### Geçerli Tekne Kompozisyonları
- ✅ **4 Hacker** (homojen grup)
- ✅ **4 Serf** (homojen grup)
- ✅ **2 Hacker + 2 Serf** (heterojen grup)
- ❌ **3 Hacker + 1 Serf** (YASAK)
- ❌ **1 Hacker + 3 Serf** (YASAK)

## 🎯 Demonstrasyon Modları

### BALANCED - Dengeli Senaryo
- **Konfigürasyon**: 8 hacker + 8 serf
- **Sonuç**: ✅ 4 sefer, tüm 16 kişi sorunsuz geçer
- **Amaç**: Senkronizasyonun doğru çalıştığını göstermek

### DEADLOCK - Deadlock Gösterimi
- **Konfigürasyon**: 2 hacker + 1 serf
- **Sonuç**: ❌ Deadlock! Geçerli tekne oluşturulamaz
- **Özellik**:
  - Deadlock detector otomatik tespit eder
  - 3 saniye sonra uyarı verir
  - 5 saniye sonra timeout ile kapanır

### STARVATION - Açlık Gösterimi
- **Konfigürasyon**: 10 hacker + 2 serf
- **Sonuç**: ⚠️ Serfler uzun süre bekler
- **Özellik**: Unfair scheduling gösterimi

### FIXED - Düzeltilmiş Versiyon
- **Konfigürasyon**: 2 hacker + 1 serf (ama timeout ile)
- **Sonuç**: ✅ Graceful degradation
- **Özellik**: Timeout mekanizması ile güvenli kapanma

## 📁 Dosya Yapısı

```
river-crossing/
├── App.java                  # Ana program, mod seçimi
├── Config.java               # Yapılandırma yönetimi
├── Logger.java               # Merkezi loglama sistemi
├── Semaphors.java            # Semafor ve durum yönetimi
├── Person.java               # Base sınıf
├── Hacker.java               # Hacker thread implementasyonu
├── Serf.java                 # Serf thread implementasyonu
├── DeadlockDetector.java     # Deadlock tespit sistemi
├── StateSnapshot.java        # Durum kayıt sınıfı
├── ThreadInfo.java           # Thread bilgi sınıfı
├── README.md                 # Bu dosya
└── DOCUMENTATION.md          # Detaylı teknik dokümantasyon
```

## 🎨 Özellikler

### ✨ Gelişmiş Özellikler
- **Renkli Loglama**: Renk kodlu terminal çıktısı
- **Deadlock Tespiti**: Otomatik deadlock tespiti ve analizi
- **Timeout Mekanizması**: Güvenli timeout ile graceful degradation
- **Detaylı İstatistikler**: Bekleme süreleri, geçiş sayıları
- **State Tracking**: Sistem durumu takibi ve history

### 📊 Loglama Seviyeleri
- `DEBUG`: Detaylı akış bilgisi (mavi)
- `INFO`: Genel bilgi (yeşil)
- `WARNING`: Uyarılar (sarı)
- `ERROR`: Hatalar (kırmızı)

## 🔍 Örnek Çıktı

### BALANCED Modu
```
✅ Geçiş #1 tamamlandı!
🚣 TEKNE KALKIYOR!
Kaptan: serf___5

Durum: ✅ BAŞARILI - Tüm kişiler karşıya geçti!
```

### DEADLOCK Modu
```
🚨 DEADLOCK TESPİT EDİLDİ! 🚨
Mevcut durum: 2 hacker + 1 serf
❌ Hiçbir geçerli kompozisyon sağlanamıyor!
💡 2H+1S durumu: 1 serf daha gelmeli ama gelmeyecek
```

## 📖 Detaylı Dokümantasyon

Detaylı teknik bilgi için [DOCUMENTATION.md](DOCUMENTATION.md) dosyasına bakın:
- Algoritma detayları
- Senkronizasyon mekanizmaları
- Deadlock analizi
- Race condition'lar
- Teorik arka plan
- İleri seviye konular

## 🧪 Test Senaryoları

```bash
# Temel test
java App BALANCED

# Deadlock testi
java App DEADLOCK

# Starvation testi
java App STARVATION

# Fixed versiyon
java App FIXED
```

## 🛠️ Gereksinimler

- Java 8 veya üzeri
- Terminal (renkli çıktı için)

## 👥 Katkı

Bu proje eğitim amaçlı bir demonstrasyondur.

## 📝 Lisans

MIT License
