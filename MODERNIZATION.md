# Modernizasyon Raporu: Java 8 → Java 25

Bu branch, `main` üzerindeki bilerek eski Java alışkanlıklarıyla yazılmış Student API'yi
[java.evolved](https://javaevolved.dev/tr/) desenleri ve java.evolved'un
[`modern-java` agent skill'i](https://javaevolved.dev/agent-plugin.html) ile modernize eder.

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

## 3. Uygulanan java.evolved desenleri

| Alan | Eski | Yeni | java.evolved |
|---|---|---|---|
| Model | Getter/setter/equals/hashCode POJO'lar | `record Student`, `Course`, `Enrollment` + compact constructor | [records-for-data-classes](https://javaevolved.dev/tr/language/records-for-data-classes.html), [compact-canonical-constructor](https://javaevolved.dev/tr/language/compact-canonical-constructor.html) |
| Durum/not | `int` sabitleri, `if/else` zinciri | `enum StudentStatus`, `enum LetterGrade(points)` | [switch-expressions](https://javaevolved.dev/tr/language/switch-expressions.html) |
| Hatalar | 3 ayrı exception, 3 kopya `catch` | `sealed ApiException` + exhaustive `switch` | [sealed-classes](https://javaevolved.dev/tr/language/sealed-classes.html), [exhaustive-switch](https://javaevolved.dev/tr/language/exhaustive-switch.html) |
| Hata ctor'u | `super()` öncesi doğrulama yapılamıyordu | Doğrulama ve alan ataması `super()` öncesinde | [flexible-constructor-bodies](https://javaevolved.dev/tr/language/flexible-constructor-bodies.html) |
| JSON yazıcı | `instanceof` + cast zinciri | Pattern matching `switch`, `case null`, guard'lar | [pattern-matching-switch](https://javaevolved.dev/tr/language/pattern-matching-switch.html), [null-in-switch](https://javaevolved.dev/tr/errors/null-in-switch.html), [guarded-patterns](https://javaevolved.dev/tr/language/guarded-patterns.html) |
| HTTP yanıtları | `status` değişkeni + dağınık `send` çağrıları | `sealed interface Response` + record pattern'ler | [record-patterns](https://javaevolved.dev/tr/language/record-patterns.html) |
| Koleksiyonlar | Raw `List`/`Map`, `Hashtable`, `Vector`, `Enumeration` | Generic'ler, `ConcurrentHashMap`, `SequencedMap`, `getFirst()/getLast()` | [raw-collections-to-generics](https://javaevolved.dev/tr/language/raw-collections-to-generics.html), [legacy-synchronized-collections](https://javaevolved.dev/tr/collections/legacy-synchronized-collections.html), [sequenced-collections](https://javaevolved.dev/tr/collections/sequenced-collections.html) |
| Değişmez listeler | `Collections.unmodifiableList(new ArrayList(...))` | `List.of`, `List.copyOf`, `Stream.toList()` | [immutable-list-creation](https://javaevolved.dev/tr/collections/immutable-list-creation.html), [copying-collections-immutably](https://javaevolved.dev/tr/collections/copying-collections-immutably.html), [stream-tolist](https://javaevolved.dev/tr/streams/stream-tolist.html) |
| Sayaçlar | `containsKey/get/put` + `new Integer(x+1)` | `Map.merge`, `groupingBy(..., summingInt(_ -> 1))` | [map-compute-and-merge](https://javaevolved.dev/tr/collections/map-compute-and-merge.html), [unnamed-variables](https://javaevolved.dev/tr/language/unnamed-variables.html) |
| Sıralama | Anonim `Comparator` sınıfları | `Comparator.comparing(...).thenComparing(...).reversed()` | [comparator-factories](https://javaevolved.dev/tr/collections/comparator-factories.html), [anonymous-classes-to-lambdas](https://javaevolved.dev/tr/language/anonymous-classes-to-lambdas.html) |
| GNO | Iterator döngüsü + `-1.0` sentinel | `mapMulti` + `Collectors.teeing` → `OptionalDouble` | [stream-mapmulti](https://javaevolved.dev/tr/streams/stream-mapmulti.html), [collectors-teeing](https://javaevolved.dev/tr/collections/collectors-teeing.html) |
| Tarih | `SimpleDateFormat` (thread-safe değil → `synchronized`), `Calendar` | `LocalDate`, katı `DateTimeFormatter`, `Period` | [java-time-basics](https://javaevolved.dev/tr/datetime/java-time-basics.html), [date-formatting](https://javaevolved.dev/tr/datetime/date-formatting.html), [duration-and-period](https://javaevolved.dev/tr/datetime/duration-and-period.html) |
| Metin | `trim().length()==0`, döngüyle `repeat`, `StringBuffer` birleştirme | `isBlank`, `strip`, `repeat`, text block + `formatted` | [string-isblank](https://javaevolved.dev/tr/strings/string-isblank.html), [string-strip](https://javaevolved.dev/tr/strings/string-strip.html), [string-repeat](https://javaevolved.dev/tr/strings/string-repeat.html), [text-blocks-for-multiline-strings](https://javaevolved.dev/tr/language/text-blocks-for-multiline-strings.html), [string-formatted](https://javaevolved.dev/tr/strings/string-formatted.html) |
| Locale | `new Locale("tr", "TR")` | `Locale.of("tr", "TR")`, `toUpperCase(Locale.ROOT)` | [locale-of](https://javaevolved.dev/tr/datetime/locale-of.html) |
| Dosya I/O | `FileReader`/`FileWriter` (platform charset), elle `finally { close }` | `Path.of`, `Files.readAllLines/write(..., UTF_8)`, try-with-resources | [path-of](https://javaevolved.dev/tr/io/path-of.html), [reading-files](https://javaevolved.dev/tr/io/reading-files.html), [explicit-charset-file-io](https://javaevolved.dev/tr/io/explicit-charset-file-io.html), [try-with-resources-effectively-final](https://javaevolved.dev/tr/io/try-with-resources-effectively-final.html) |
| HTTP gövdesi | `byte[4096]` döngüsü, `URLDecoder.decode(s, "UTF-8")` | `readAllBytes()`, `URLDecoder.decode(s, UTF_8)` | [inputstream-transferto](https://javaevolved.dev/tr/io/inputstream-transferto.html) |
| Güvenlik | `new Random()` ile API anahtarı, elle hex, `equals` | `SecureRandom`, `HexFormat`, sabit zamanlı `MessageDigest.isEqual` | [strong-random](https://javaevolved.dev/tr/security/strong-random.html), [hex-format](https://javaevolved.dev/tr/datetime/hex-format.html) |
| Yansıma | `Class.newInstance()` | `asSubclass(...).getDeclaredConstructor().newInstance()` | [class-newinstance-to-constructor](https://javaevolved.dev/tr/tooling/class-newinstance-to-constructor.html) |
| Kimlik üretimi | `volatile`'sız double-checked locking singleton | Depo başına `AtomicLong` | [lock-free-lazy-init](https://javaevolved.dev/tr/concurrency/lock-free-lazy-init.html) |
| Bildirim kuyruğu | `LinkedList` + `wait/notify`, yutulan `InterruptedException` | `BlockingQueue`, sanal thread, kooperatif iptal | [wait-notify-to-blocking-queue](https://javaevolved.dev/tr/concurrency/wait-notify-to-blocking-queue.html), [thread-stop-to-cooperative-cancellation](https://javaevolved.dev/tr/concurrency/thread-stop-to-cooperative-cancellation.html) |
| Uyku | `Thread.sleep(50)` | `Thread.sleep(Duration)` | [thread-sleep-duration](https://javaevolved.dev/tr/concurrency/thread-sleep-duration.html) |
| Zamanlayıcı | `Timer` + `TimerTask` | `ScheduledExecutorService` + `Duration` | [timer-task-to-scheduled-executor](https://javaevolved.dev/tr/concurrency/timer-task-to-scheduled-executor.html) |
| HTTP sunucu | `newFixedThreadPool(10)` | `newVirtualThreadPerTaskExecutor()` | [virtual-threads](https://javaevolved.dev/tr/concurrency/virtual-threads.html) |
| İstek bağlamı | — | `ScopedValue<RequestContext>` (Java 25 final) | [scoped-values](https://javaevolved.dev/tr/concurrency/scoped-values.html) |
| Null varsayılanları | `x != null ? x : y` | `Objects.requireNonNullElse` | [require-nonnull-else](https://javaevolved.dev/tr/errors/require-nonnull-else.html) |
| Optional | `null` dönüşleri + kontrol | `Optional` dönüşleri, `orElseThrow` | [optional-orelsethrow](https://javaevolved.dev/tr/errors/optional-orelsethrow.html) |
| Kullanılmayan değişkenler | `catch (NumberFormatException e)` | `catch (NumberFormatException _)` | [unnamed-variables](https://javaevolved.dev/tr/language/unnamed-variables.html) |
| Dokümantasyon | `/** HTML javadoc */` | `/// Markdown` yorumları | [markdown-javadoc-comments](https://javaevolved.dev/tr/language/markdown-javadoc-comments.html) |
| Script | — | `scripts/HealthCheck.java`: compact source file + `import module` + `IO.println` + `HttpClient` | [compact-source-files](https://javaevolved.dev/tr/language/compact-source-files.html), [module-import-declarations](https://javaevolved.dev/tr/language/module-import-declarations.html), [io-class-console-io](https://javaevolved.dev/tr/io/io-class-console-io.html), [http-client](https://javaevolved.dev/tr/io/http-client.html) |
| Testler | JUnit 4, `@Test(expected=...)`, `try/fail/catch` | JUnit 6, `assertThrows`, `@ParameterizedTest`, sabit `Clock` | [junit6-with-jspecify](https://javaevolved.dev/tr/tooling/junit6-with-jspecify.html) |

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
