#!/usr/bin/env bash
# Uygulamayı geçici bir veri dosyasıyla başlatır, tüm uç noktaları çağırır
# ve yanıtları standart çıktıya yazar. İki sürümün çıktısını karşılaştırmak için:
#   ./scripts/smoke-test.sh > out.txt
set -euo pipefail

PORT="${PORT:-18080}"
KEY="dev-secret-key"
BASE="http://localhost:${PORT}/api"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"

cp "$ROOT/data/students.csv" "$TMP/students.csv"
(cd "$ROOT" && mvn -q -DskipTests package >/dev/null)

java -Dserver.port="$PORT" -Drepository.file="$TMP/students.csv" -jar "$ROOT/target/student-api.jar" \
  >"$TMP/server.log" 2>&1 &
PID=$!
trap 'kill $PID 2>/dev/null || true; rm -rf "$TMP"' EXIT

for _ in $(seq 1 50); do
  curl -s "$BASE/health" >/dev/null 2>&1 && break
  sleep 0.2
done

call() {
  local title="$1"; shift
  echo "### $title"
  curl -s -w '\n[HTTP %{http_code}]\n' "$@"
  echo
}

call "health"                     "$BASE/health"
call "kurslar"                    "$BASE/courses"
call "tüm öğrenciler"             "$BASE/students"
call "filtre + sıralama"          "$BASE/students?department=Bilgisayar%20M%C3%BChendisli%C4%9Fi&status=active&sort=gpa"
call "isim sıralaması"            "$BASE/students?sort=name"
call "geçersiz sıralama"          "$BASE/students?sort=boy"
call "tek öğrenci"                "$BASE/students/1"
call "olmayan öğrenci"            "$BASE/students/999"
call "geçersiz id"                "$BASE/students/abc"
call "anahtarsız oluşturma"       -X POST "$BASE/students" -d '{}'
call "hatalı json"                -X POST -H "X-API-Key: $KEY" "$BASE/students" -d '{"firstName": '
call "doğrulama hataları"         -X POST -H "X-API-Key: $KEY" "$BASE/students" -d '{"email":"x","birthDate":"2003-02-30","status":"UZAYDA"}'
call "öğrenci oluştur"            -X POST -H "X-API-Key: $KEY" "$BASE/students" \
  -d '{"firstName":" ismail ","lastName":"KARA","email":"Ismail.Kara@Ogrenci.edu.tr","birthDate":"2004-03-15","department":"Fizik"}'
call "derse kaydol"               -X POST -H "X-API-Key: $KEY" "$BASE/students/9/enrollments" -d '{"courseCode":"phys101","semester":"2025-GUZ"}'
call "tekrar kaydol"              -X POST -H "X-API-Key: $KEY" "$BASE/students/9/enrollments" -d '{"courseCode":"PHYS101","semester":"2025-GUZ"}'
call "geçersiz dönem"             -X POST -H "X-API-Key: $KEY" "$BASE/students/9/enrollments" -d '{"courseCode":"PHYS210","semester":"2025-KIS"}'
call "not gir"                    -X PUT -H "X-API-Key: $KEY" "$BASE/students/9/enrollments/PHYS101/grade" -d '{"letterGrade":"ba"}'
call "geçersiz not"               -X PUT -H "X-API-Key: $KEY" "$BASE/students/9/enrollments/PHYS101/grade" -d '{"letterGrade":"A+"}'
call "gno"                        "$BASE/students/9/gpa"
call "askıdaki öğrenci kaydı"     -X POST -H "X-API-Key: $KEY" "$BASE/students/5/enrollments" -d '{"courseCode":"MATH101","semester":"2025-GUZ"}'
call "geçmiş dersi tekrar alma"   -X POST -H "X-API-Key: $KEY" "$BASE/students/1/enrollments" -d '{"courseCode":"CENG101","semester":"2025-GUZ"}'
call "güncelle"                   -X PUT -H "X-API-Key: $KEY" "$BASE/students/6" -d '{"lastName":"öztürk","department":"Matematik"}'
call "gnosuz mezuniyet"           -X PUT -H "X-API-Key: $KEY" "$BASE/students/6" -d '{"status":"GRADUATED"}'
call "mezuniyet"                  -X PUT -H "X-API-Key: $KEY" "$BASE/students/3" -d '{"status":"GRADUATED"}'
call "notlu öğrenciyi sil"        -X DELETE -H "X-API-Key: $KEY" "$BASE/students/1"
call "notsuz öğrenciyi sil"       -X DELETE -H "X-API-Key: $KEY" "$BASE/students/7"
call "transkript"                 "$BASE/students/1/transcript"
call "gno (ortalama)"             "$BASE/students/2/gpa"
call "bilinmeyen alt kaynak"      "$BASE/students/1/foo"
call "istatistikler"              "$BASE/stats"
