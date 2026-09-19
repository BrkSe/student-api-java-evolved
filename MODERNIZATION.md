# Modernizasyon Raporu: Java 8 → Java 25

Bu branch, `main` üzerindeki bilerek eski Java alışkanlıklarıyla yazılmış Student API'yi
java.evolved  desenleri ve java.evolved'un
`modern-java` agent skill'i  ile modernize eder.

## 1. Skill ile hedef tespiti

`modern-java` skill'i önce projenin **gerçek** derleme hedefini tespit eder; makinede kurulu JDK'yı değil:

```text
$ detect-java-version.sh .
selected: 8  (source: maven-source, pom.xml, raw "1.8", priority 70)
candidates: 8 (maven-source), 26 (runtime — yalnızca yedek kanıt, priority 10)
```

Kurulu JDK 26 olmasına rağmen öneriler Java 8 ile sınırlanır. Modernizasyon hedefi açıkça verildi:

```text
$ detect-java-version.sh . --java-version 25
selected: 25 (source: explicit, priority 100)
conflicts: 8 (maven-source, pom.xml)
```

Skill kurallarına göre:

- Hedef yükseltme ile kod refactor'ı **ayrı commit'lerde** yapıldı (`build: derleme hedefini Java 25 LTS'e yükselt`).
- Yalnızca Java 25'te **final** olan özellikler kullanıldı. Preview özellikler (structured concurrency,
  stable values, primitive patterns, PEM API) bilinçli olarak **kullanılmadı**.
- Doğrulama projenin kendi build'i ile `maven.compiler.release=25` üzerinden yapıldı.

## 2. Sonuçlar

| Ölçüm | `main` (Java 8) | `modernize/java-25` |
|---|---|---|
| `javac -Xlint:all` uyarısı (serial hariç) | 269 | 0 |
| Üretim kodu (Java satırı) | 2589 | 1963 (−%24) |
| Birim test | 19 (JUnit 4) | 23 (JUnit 6) |
| `smoke-test.sh` — 30 HTTP senaryosu | referans çıktı | **birebir aynı** |

## 4. Bilinçli olarak dokunulmayanlar

Skill "her eski yapıyı mekanik olarak değiştirme" der:

- **`enroll` içindeki kayıt çakışma döngüsü** stream'e çevrilmedi: ilk eşleşen kayıt hangi hatanın
  döneceğini belirliyor, erken çıkışlı döngü daha net.
- **Ortalama GNO** `DoubleStream.average()` ile değil sıralı toplama ile hesaplanıyor; telafili toplama
  2 haneli yuvarlamada eski sürümden farklı sonuç üretebilirdi.
- **Preview özellikler** (structured concurrency, stable values, primitive patterns) Java 25'te final
  olmadığı için kullanılmadı.
- **Harici JSON kütüphanesi** eklenmedi; projenin "bağımlılıksız" sözleşmesi korundu.

## 5. Modernizasyonun yakaladığı gizli hatalar

1. **Kısmi güncelleme:** `PUT /api/students/6 {"lastName":"değişti","status":"GRADUATED"}` isteği
   `409 Conflict` dönmesine rağmen eski sürümde soyadını değiştiriyordu (mutable nesne, doğrulamadan önce
   `setLastName`). Record'larla yeni örnek yalnızca tüm kurallar geçerse kaydediliyor
   (`failedUpdateDoesNotLeavePartialChanges` testi).
2. **Zayıf rastgelelik:** API anahtarı `java.util.Random` ile üretiliyordu → `SecureRandom`.
3. **Zamanlama saldırısı:** anahtar `String.equals` ile karşılaştırılıyordu → `MessageDigest.isEqual`.
4. **Thread güvenliği:** `volatile` olmayan double-checked locking singleton ve paylaşılan `SimpleDateFormat`.
5. **Locale hatası:** `toUpperCase()`/`toLowerCase()` varsayılan locale ile çağrılıyordu; JVM Türkçe locale
   ile çalışırsa `ISMAIL@...` → `ısmaıl@...` olurdu → `Locale.ROOT`.
6. **Karakter kodlaması:** `FileReader`/`FileWriter` platform charset'ine bağlıydı → açık `UTF_8`.
7. **Yutulan kesinti:** `InterruptedException` sessizce yutuluyordu → kesinti durumu korunuyor.
