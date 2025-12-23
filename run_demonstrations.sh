#!/bin/bash

# River Crossing Problem - Tüm Demonstrasyonları Çalıştır
# Bu script tüm modları sırayla çalıştırır ve sonuçları gösterir

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║    RIVER CROSSING PROBLEM - TÜM DEMONSTRASYONLAR             ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Java'nın PATH'te olduğundan emin ol
export PATH="/usr/local/opt/openjdk/bin:$PATH"

# Önce derle
echo "📦 Java dosyaları derleniyor..."
javac *.java

if [ $? -ne 0 ]; then
    echo "❌ Derleme hatası! Çıkılıyor..."
    exit 1
fi

echo "✅ Derleme başarılı!"
echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""

# 1. BALANCED Modu
echo "1️⃣  BALANCED MODU ÇALIŞTIRILIYOR..."
echo "   (8 hacker + 8 serf - Dengeli senaryo)"
echo ""
java App BALANCED
echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""
read -p "Devam etmek için Enter'a basın..."
echo ""

# 2. DEADLOCK Modu
echo "2️⃣  DEADLOCK MODU ÇALIŞTIRILIYOR..."
echo "   (2 hacker + 1 serf - Deadlock gösterimi)"
echo "   ⚠️  Bu mod 5 saniye sonra timeout olacak"
echo ""
java App DEADLOCK
echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""
read -p "Devam etmek için Enter'a basın..."
echo ""

# 3. STARVATION Modu
echo "3️⃣  STARVATION MODU ÇALIŞTIRILIYOR..."
echo "   (10 hacker + 2 serf - Starvation gösterimi)"
echo ""
java App STARVATION
echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""
read -p "Devam etmek için Enter'a basın..."
echo ""

# 4. FIXED Modu
echo "4️⃣  FIXED MODU ÇALIŞTIRILIYOR..."
echo "   (2 hacker + 1 serf - Düzeltilmiş versiyon)"
echo "   ✅ Timeout mekanizması ile graceful degradation"
echo ""
java App FIXED
echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""

echo "✅ Tüm demonstrasyonlar tamamlandı!"
echo ""
echo "📖 Daha fazla bilgi için:"
echo "   - README.md (hızlı başlangıç)"
echo "   - DOCUMENTATION.md (detaylı teknik dokümantasyon)"
echo ""
