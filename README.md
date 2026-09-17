# Student API

Öğrenci, ders kaydı, not ve genel not ortalaması (GNO) yönetimi için **hiçbir framework kullanmayan** bir REST API.

> ✅ **Bu branch, Java 25 LTS ile modernize edilmiş sürümdür.**
> Eski Java 8 sürümü `main` branch'indedir. Modernizasyon [java.evolved](https://javaevolved.dev/tr/)
> desenleri ve java.evolved'un [`modern-java` agent skill'i](https://javaevolved.dev/agent-plugin.html)
> ile yapılmıştır. Hangi desenin nereye uygulandığı ve sonuçlar için
> **[MODERNIZATION.md](MODERNIZATION.md)** dosyasına bakın.

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

- JDK 25+
- Maven 3.9+

## Çalıştırma

```bash
mvn package
java -jar target/student-api.jar
```

Yapılandırma `src/main/resources/application.properties` dosyasındadır ve `-Danahtar=değer` ile ezilebilir:

| Anahtar | Varsayılan | Açıklama |
|---|---|---|
| `server.port` | `8080` | HTTP portu |
| `api.key` | `dev-secret-key` | Yazma işlemleri için API anahtarı (boşsa rastgele üretilir) |
| `repository.class` | `FileStudentRepository` | Depo implementasyonu |
| `repository.file` | `data/students.csv` | CSV veri dosyası |
| `backup.period.millis` | `60000` | Yedekleme periyodu |

## Uç noktalar

| Metot | Yol | Açıklama |
|---|---|---|
| GET | `/api/health` | Sağlık kontrolü |
| GET | `/api/courses` | Ders kataloğu |
| GET | `/api/stats` | İstatistikler |
| GET | `/api/students?department=&status=&sort=id\|name\|gpa\|age` | Listele / filtrele / sırala |
| POST | `/api/students` | Öğrenci oluştur |
| GET | `/api/students/{id}` | Öğrenci detayı |
| PUT | `/api/students/{id}` | Kısmi güncelleme |
| DELETE | `/api/students/{id}` | Sil (notlu dersi yoksa) |
| POST | `/api/students/{id}/enrollments` | Derse kaydol |
| PUT | `/api/students/{id}/enrollments/{courseCode}/grade` | Not gir |
| GET | `/api/students/{id}/gpa` | GNO özeti |
| GET | `/api/students/{id}/transcript` | Transkript (text/plain) |

### Örnek

```bash
curl -X POST http://localhost:8080/api/students \
  -H "X-API-Key: dev-secret-key" \
  -d '{"firstName":"ismail","lastName":"KARA","email":"ismail.kara@ogrenci.edu.tr","birthDate":"2004-03-15","department":"Fizik"}'

curl -X POST http://localhost:8080/api/students/9/enrollments \
  -H "X-API-Key: dev-secret-key" \
  -d '{"courseCode":"PHYS101","semester":"2025-GUZ"}'

curl http://localhost:8080/api/students/1/transcript
```

## Testler

```bash
mvn test                      # birim testleri
./scripts/smoke-test.sh       # uygulamayı başlatıp tüm uç noktaları çağırır
java scripts/HealthCheck.java # çalışan API'ye sağlık kontrolü (Java 25 compact source file)
```

`smoke-test.sh` çıktısı iki branch arasında davranışın korunduğunu doğrulamak için birebir karşılaştırılabilir.
