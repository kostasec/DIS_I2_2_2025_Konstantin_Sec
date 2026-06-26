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
Senzor
  ↓ HTTP POST
ingest-service (8085)
  ↓ Kafka: raw-measurements
processing-service (8086)
  ↓ valid-measurements        ↓ threshold-breaches
monitoring-service (8082)   alert-service (8083)
  ↑                               ↑
  └─── telemetry-composite (8084) ┘
              ↑
  device-registry (8081) ← MySQL
```

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
GET  /device/{id}     — dohvati uređaj
POST /device          — registruj uređaj
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

| Parametar | Prag |
|-----------|------|
| CO | > 0.01 |
| Temperatura | > 40°C |

---

## Pokretanje

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

```bash
# Dodaj uređaj
curl -X POST http://localhost:8081/device \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"sensor-009","name":"Smoke Detector","location":"Server Room","type":"smoke"}'

# Pošalji merenje (alarm)
curl -X POST http://localhost:8085/ingest \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"sensor-009","temperature":25.3,"humidity":60.0,"co":0.015,"lpg":0.007,"smoke":0.02,"light":true,"motion":false}'

# Proveri stanje
curl http://localhost:8082/monitoring/sensor-009/state

# Proveri alarme
curl http://localhost:8083/alert/sensor-009

# Kompletan odgovor
curl http://localhost:8084/telemetry/sensor-009
```

---

## Mapiranje na literaturu

### Larsson (vežbe)
- Composite obrazac (poglavlje 3) → `telemetry-composite-service`
- Spring Cloud Stream producer → `ingest-service` (StreamBridge)
- Spring Cloud Stream consumer → `monitoring-service`, `alert-service` (@Bean Consumer)
- Spring Cloud Stream function → `processing-service` (@Bean Function)

### van Steen (predavanja)
- 2.1.2 SOA → REST komunikacija između servisa
- 2.1.3 Publish-subscribe → Kafka topici
- 4.3.3 Message-oriented persistent → asinhrona Kafka komunikacija
- 3.2.2 Kontejneri → Docker i Docker Compose

---

## Tehnologije

- Java 17
- Spring Boot 3.x
- Spring Cloud Stream
- Apache Kafka
- MySQL 8.0
- Docker / Docker Compose
- Gradle