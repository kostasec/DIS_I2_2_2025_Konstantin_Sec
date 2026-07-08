# DIS_I2_2_2025_Konstantin_Sec

**Ime i prezime:** Konstantin Šec  
**Broj indeksa:** I2 2/2025  
**Predmet:** Distribuirani informacioni sistemi  
**Akademska godina:** 2025/2026  

---

## Opis projekta

Projekat predstavlja implementaciju distribuiranog informacionog sistema za prikupljanje, obradu i nadzor IoT telemetrijskih podataka u realnom vremenu. Sistem je realizovan primenom mikroservisne arhitekture zasnovane na Spring Boot i Spring Cloud tehnologijama, uz upotrebu Apache Kafka platforme za asinhroni prenos poruka između servisa.

Dataset: [Environmental Sensor Telemetry Data](https://www.kaggle.com/datasets/garystafford/environmental-sensor-data-132k) — ~405.000 merenja sa 4 IoT uređaja (temperatura, vlažnost, CO, LPG, dim, svetlo, pokret).

---

## Arhitektura

Sistem se sastoji od dva sloja:

**Operativni sloj** — Spring Cloud mikroservisi za realtime monitoring i alarme  
**Komunikacija** — sinhrona (REST) i asinhrona (Apache Kafka)

```
Senzor / simulator
  ↓ HTTP POST (preko Gateway 8080)
ingest-service (8085)
  ↓ Kafka: raw-measurements
processing-service (8086) ──sync REST: pragovi──► device-registry (8081) ← MySQL
  ↓ valid-measurements        ↓ threshold-breaches
monitoring-service (8082)   alert-service (8083)
  ↓ Redis (hash: stanje)      ↓ Redis (lista: alarmi)
  ↑                               ↑
  └─── telemetry-composite (8084) ┘   (agregira sva tri izvora)
```

**Komunikacija:**
- **Asinhrona (Kafka):** ingest → processing → monitoring/alert
- **Sinhrona (REST):** processing → device-registry (pragovi), telemetry-composite → monitoring/alert/registry
- Sve preko **Eureka** service discovery-ja; sinhroni pozivi koriste klijentski load balancing

> 📄 Detaljan opis arhitekture, dijagrami (Mermaid) i tok podataka: [docs/arhitektura-i-tok.md](docs/arhitektura-i-tok.md)

---

## Mikroservisi

| Servis | Port | Opis |
|--------|------|------|
| device-registry-service | 8081 | Registar uređaja — MySQL baza |
| ingest-service | 8085 | Prima merenja, objavljuje na Kafka |
| processing-service | 8086 | Evaluira pragove, rutira poruke |
| monitoring-service | 8082 | Živo stanje uređaja |
| alert-service | 8083 | Kreiranje i praćenje alarma |
| telemetry-composite-service | 8084 | Agregira odgovore svih servisa |

---

## REST API

### device-registry-service
```
GET  /device/{id}             — dohvati uređaj
GET  /device/{id}/thresholds  — pragovi alarma za uređaj (koristi processing-service)
POST /device                  — registruj uređaj
```

### ingest-service
```
POST /ingest          — primi merenje i objavi na Kafka
```

### monitoring-service
```
GET  /monitoring/{deviceId}/state  — trenutno stanje uređaja
```

### alert-service
```
GET  /alert/{deviceId}  — lista alarma za uređaj
```

### telemetry-composite-service
```
GET  /telemetry/{deviceId}  — kompletan agregiran odgovor
```

---

## Kafka topici

| Topic | Producer | Consumer |
|-------|----------|----------|
| raw-measurements | ingest-service | processing-service |
| valid-measurements | processing-service | monitoring-service |
| threshold-breaches | processing-service | alert-service |

---

## Pragovi alarma

Pragovi su **specifični po uređaju** i čuvaju se u `device-registry-service` (MySQL).
`processing-service` ih **sinhrono povlači** preko REST-a (`GET /device/{id}/thresholds`),
uz klijentski load balancing kroz Eureka service discovery. Ako registry nije dostupan,
koristi se podrazumevani prag (CO > 0.01, Temp > 40°C). Merenje se rutira na
`threshold-breaches` ako premaši prag, inače na `valid-measurements`.

| Uređaj | Lokacija | Prag CO | Prag temperature |
|--------|----------|---------|------------------|
| b8:27:eb:bf:9d:51 | Living Room | > 0.01 | > 40°C |
| 00:0f:00:70:91:0a | Kitchen | > 0.02 | > 45°C |
| 1c:bf:ce:15:ec:4d | Bedroom | > 0.01 | > 35°C |

---

## Pokretanje

> 📄 Detaljno uputstvo za pipeline (build/test/deploy, dev vs prod): [docs/pipeline.md](docs/pipeline.md)

### Preduslovi
- Docker Desktop
- Java 17
- Git

### Pokretanje sistema

```bash
git clone https://github.com/kostasec/DIS_I2_2_2025_Konstantin_Sec.git
cd DIS_I2_2_2025_Konstantin_Sec

# Build svih servisa
cd device-registry-service && ./gradlew build -x test && cd ..
cd ingest-service && ./gradlew build -x test && cd ..
cd processing-service && ./gradlew build -x test && cd ..
cd monitoring-service && ./gradlew build -x test && cd ..
cd alert-service && ./gradlew build -x test && cd ..
cd telemetry-composite-service && ./gradlew build -x test && cd ..

# Pokretanje
docker compose up -d
```

### Testiranje

Uređaji se automatski registruju na startu (`DataInitializer`), sa pragovima iz tabele gore.

```bash
# Normalno merenje -> ide na monitoring (Living Room, prag temp 40)
curl -X POST http://localhost:8080/ingest \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"b8:27:eb:bf:9d:51","temperature":22,"co":0.004}'
curl http://localhost:8082/monitoring/b8:27:eb:bf:9d:51/state

# Breach merenje -> ide na alert (Bedroom, prag temp 35, pa temp=37 premašuje)
curl -X POST http://localhost:8080/ingest \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"1c:bf:ce:15:ec:4d","temperature":37,"co":0.005}'
curl http://localhost:8083/alert/1c:bf:ce:15:ec:4d

# Kompletan agregiran odgovor
curl http://localhost:8084/telemetry/b8:27:eb:bf:9d:51
```

> Isto merenje (`temp=37`) je breach za Bedroom (prag 35) ali ne i za Living Room (prag 40) —
> demonstracija pragova po uređaju povučenih iz registry-ja.

Za masovno testiranje pokreni simulator (replay dataset-a):
```bash
py simulator.py
```

### Pokretanje testova (unit + integracioni)

Svi servisi sa testovima odjednom (iz root foldera):
```cmd
run-all-tests.bat
```
Ili po servisu: `gradlew test`.

> Integracioni testovi (processing/monitoring/device-registry) koriste **Testcontainers** i traže **pokrenut Docker**. Na Docker-u 29+ je u `test` task-ovima pinovana API verzija (`api.version=1.41`) jer podrazumevana docker-java verzija puca sa HTTP 400. Detalji: [docs/pipeline.md](docs/pipeline.md).

---

## Mapiranje na literaturu

### Larsson (vežbe)
- Composite obrazac (poglavlje 3) → `telemetry-composite-service`
- Spring Cloud Stream producer → `ingest-service` (StreamBridge)
- Spring Cloud Stream consumer → `monitoring-service`, `alert-service` (@Bean Consumer)
- Spring Cloud Stream function → `processing-service` (@Bean Function)

### van Steen (predavanja)
- 2.1.2 SOA → REST komunikacija između servisa (processing → device-registry za pragove)
- 2.1.3 Publish-subscribe → Kafka topici
- 4.3.3 Message-oriented persistent → asinhrona Kafka komunikacija
- 3.2.2 Kontejneri → Docker i Docker Compose
- Imenovanje i lociranje → Eureka service discovery + klijentski load balancing

---

## Tehnologije

- Java 17
- Spring Boot 3.4.5
- Spring Cloud 2024.0.1 (Stream, Netflix Eureka, LoadBalancer, Gateway)
- Apache Kafka (asinhrona komunikacija)
- MySQL 8.0 (registar uređaja i pragovi)
- Redis 7.2 (živo stanje i alarmi)
- Docker / Docker Compose
- Gradle