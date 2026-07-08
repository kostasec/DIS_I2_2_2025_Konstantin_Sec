# Pipeline: build / test / deploy

Uputstvo za upravljanje pipeline-om sistema kroz faze, sa razdvajanjem **razvojnog (dev)** i **produkcionog (prod)** okruženja.

---

## Pregled faza

```
BUILD            TEST                 PACKAGE              DEPLOY
gradlew bootJar  gradlew test         docker compose      docker compose up -d
(jar po servisu) (unit + integr.)     build (slike)       (dev ili prod)
```

Svaki mikroservis je **nezavisan Gradle projekat** (nema root `settings.gradle`) sa sopstvenim `gradlew` wrapper-om. Sedam servisa koristi Groovy DSL (`build.gradle`), a `device-registry-service` Kotlin DSL (`build.gradle.kts`). Svi: Java 17, Spring Boot 3.4.5, Spring Cloud 2024.0.1.

---

## Preduslovi

- **Docker Desktop** (pokrenut) — za kontejnere i za integracione testove
- **Java 17** (JDK)
- Gradle nije potreban globalno — koristi se `gradlew` wrapper iz svakog servisa

---

## 1. BUILD — izgradnja jar-ova

Dockerfile svakog servisa je jednostavan i **kopira već izgrađen jar**:
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```
Zbog toga se jar mora izgraditi **pre** pravljenja Docker slika.

Po servisu (iz foldera servisa):
```bash
gradlew bootJar
```

Svih 8 odjednom (iz root foldera, Windows `cmd`):
```cmd
for %s in (eureka-server gateway-service device-registry-service monitoring-service alert-service telemetry-composite-service ingest-service processing-service) do @(cd %s && call gradlew.bat bootJar & cd ..)
```

Rezultat: `<servis>/build/libs/<servis>-0.0.1-SNAPSHOT.jar`.

---

## 2. TEST — unit i integracioni testovi

Jednom komandom, svi servisi sa testovima (iz root foldera):
```cmd
run-all-tests.bat
```

Ili po servisu:
```bash
gradlew test
```

**Vrste testova:**

| Servis | Unit | Integracioni (Testcontainers) | Traži Docker? |
|--------|------|-------------------------------|---------------|
| processing-service | ✅ | ✅ Kafka | da |
| monitoring-service | ✅ | ✅ Redis | da |
| device-registry-service | ✅ | ✅ MySQL | da |
| alert-service | ✅ | — | ne |
| ingest-service | ✅ | — | ne |

Integracioni testovi koriste **Testcontainers** — dižu prave Kafka/Redis/MySQL kontejnere tokom testa, pa **Docker mora biti pokrenut**.

> **Napomena za Docker 29+ (Windows):** noviji Docker zahteva API verziju ≥ 1.40, a podrazumevana docker-java verzija (1.32) puca sa HTTP 400. Zato je u `test` task-ovima servisa sa integracionim testovima pinovana kompatibilna verzija (`systemProperty 'api.version', '1.41'`). Radi na Docker-u 19.03–29. Ako grader ima stariji Docker, može override-ovati sa `set DOCKER_API_VERSION=...`.

HTML izveštaj po servisu:
```
<servis>/build/reports/tests/test/index.html
```

---

## 3. PACKAGE — Docker slike

Pošto su jar-ovi izgrađeni, slike se prave iz `docker-compose.yml` (`build.context` po servisu):
```bash
docker compose build
```
Ovo gradi sliku za svaki app servis iz njegovog `build/libs/*.jar`. Infrastruktura (MySQL, Redis, Kafka, Zookeeper) koristi gotove slike sa Docker Hub-a i ne gradi se.

---

## 4. DEPLOY — pokretanje

### 4.1. Razvoj (dev)

Bazni `docker-compose.yml` je razvojno okruženje:
```bash
docker compose up -d
```

Karakteristike dev-a:
- Servisi se **grade iz izvornog koda** (`build.context`)
- Kredencijali baze su plaintext u compose fajlu (`dis123`)
- `ddl-auto=update`, `show-sql=true` (Hibernate menja šemu i loguje SQL — pogodno za razvoj)
- Svi portovi izloženi na host (lako debagovanje)

Provera:
```bash
docker compose ps                 # svi kontejneri Up
```
Eureka dashboard: <http://localhost:8761> (svi servisi `UP`).
Test toka podataka: `python simulator.py` pa `curl http://localhost:8082/monitoring/<id>/state`.

### 4.2. Produkcija (prod)

Prod se dobija kombinovanjem baznog compose-a sa **prod override-om** (`docker-compose.prod.yml`).

1. Napravi `.env` sa tajnama (ne komituje se u git):
   ```cmd
   copy .env.example .env
   ```
   pa izmeni lozinke u `.env`.

2. Pokreni sa oba fajla:
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
   ```

Šta prod override menja:
- **Tajne iz `.env`** umesto plaintext-a (`MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `SPRING_DATASOURCE_PASSWORD`)
- **`SPRING_PROFILES_ACTIVE=prod`** na svim app servisima → device-registry aktivira `application-prod.properties` (`ddl-auto=validate`, `show-sql=false`)
- **`restart: unless-stopped`** na svim kontejnerima (samo-oporavak posle pada ili restarta hosta)

---

## Dev vs Prod — pregled razlika

| Aspekt | Dev (`docker-compose.yml`) | Prod (`+ docker-compose.prod.yml`) |
|--------|----------------------------|------------------------------------|
| Kredencijali | plaintext u fajlu (`dis123`) | iz `.env` (van git-a) |
| Spring profil | podrazumevani | `prod` |
| Hibernate šema | `ddl-auto=update` | `ddl-auto=validate` |
| SQL logovi | `show-sql=true` | `show-sql=false` |
| Restart politika | nema | `unless-stopped` |
| Pokretanje | `docker compose up -d` | `docker compose -f ... -f docker-compose.prod.yml up -d` |

---

## Napomene i ograničenja

- **Šema u produkciji:** `ddl-auto=validate` zahteva da tabele **već postoje** — Hibernate ih više ne kreira. Pri prvom prod deploy-u na praznu bazu treba prethodno postaviti šemu (npr. pokretanjem dev-a jednom, ili — u pravoj produkciji — alatom za migracije poput **Flyway** ili **Liquibase**). Ovaj projekat namerno ostavlja migracije kao sledeći korak.
- **Infra portovi:** Docker Compose override ne može da *ukloni* portove definisane u baznom fajlu (liste portova se spajaju, ne zamenjuju). Zato su u prod-u infra portovi i dalje izloženi; pravo skrivanje bi zahtevalo restrukturiranje baznog compose-a ili poseban prod compose bez `ports`.
## CI/CD (GitHub Actions)

Pipeline je automatizovan preko GitHub Actions-a — [`.github/workflows/ci.yml`](../.github/workflows/ci.yml). Pokreće se na svaki `push` i `pull_request` ka `main`:

- **`test`** — matrica preko 6 servisa sa testovima; svaki `./gradlew test`. Runner (`ubuntu-latest`) ima Docker, pa **Testcontainers integracioni testovi rade** u CI-ju.
- **`package`** — matrica preko svih 8 servisa; svaki `./gradlew bootJar` (dokaz da sve pakuje).

Status build-a je vidljiv kao badge na vrhu `README.md`. Naredni prirodni korak (nije obavezan) bio bi `deploy` job koji gradi Docker slike i objavljuje ih u registar.
