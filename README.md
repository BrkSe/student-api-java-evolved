Blog Yazısına Ulaş! [java.evolved Nedir? Modern Java Agent Skill ile Java 8 Kodunu Java 25’e Taşımak](https://blog.burakkutbay.com/java-evolved-nedir-eski-java-kodunu-java-25e-tasimak.html/)

# Student API

Öğrenci, ders kaydı, not ve genel not ortalaması (GNO) yönetimi için **hiçbir framework kullanmayan** bir REST API.

> ⚠️ **Bu branch bilerek eski Java alışkanlıklarıyla yazılmıştır.**
> Raw type koleksiyonlar, `Hashtable`/`Vector`, anonim sınıflar, `SimpleDateFormat`/`Calendar`,
> `wait`/`notify`, `Timer`, `StringBuffer`, `new Integer(...)`, `Class.newInstance()`, elle yazılmış
> `try/finally` blokları ve `instanceof` + cast zincirleri...
>
> Bu kod tabanı, java.evolved desenleri ve java.evolved'un
> `modern-java` agent skill'i kullanılarak
> modernize edilmiştir. Modern sürüm için **`modernize/java-25`** branch'ine bakın.

## Özellikler

- Öğrenci CRUD (doğrulama, e-posta benzersizliği, Türkçe karakterlere duyarlı isim düzeltme)
- Ders kataloğu, dönem bazlı ders kaydı (kredi limiti, tekrar alma kuralları)
- Harf notu girişi (AA…FF), kredi ağırlıklı GNO ve onur derecesi
- Düz metin transkript
- İstatistikler: duruma/bölüme göre dağılım, ilk 3 öğrenci, not dağılımı
- CSV dosyasına kalıcılık + periyodik yedekleme
- Arka planda e-posta bildirim kuyruğu
- Yazma işlemleri için `X-API-Key` doğrulaması

## Gereksinimler

- JDK 8+
- Maven 3.9+

## Çalıştırma

```bash
mvn package
java -jar target/student-api.jar
```
