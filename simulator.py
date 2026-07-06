import csv
import requests
import time

INGEST_URL = "http://localhost:8080/ingest"
CSV_FILE = "data-source/iot_telemetry_data.csv"
DELAY_SECONDS = 0.1
MAX_ROWS = 2000

def send_measurement(row):
    payload = {
        "deviceId": row["device"],
        "temperature": float(row["temp"]),
        "humidity": float(row["humidity"]),
        "co": float(row["co"]),
        "lpg": float(row["lpg"]),
        "smoke": float(row["smoke"]),
        "light": row["light"].strip().lower() == "true",
        "motion": row["motion"].strip().lower() == "true"
    }
    try:
        response = requests.post(INGEST_URL, json=payload, timeout=5)
        print(f"[{row['ts']}] {row['device']} -> {response.status_code}")
    except Exception as e:
        print(f"Error: {e}")

def main():
    count = 0
    with open(CSV_FILE, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            send_measurement(row)
            count += 1
            time.sleep(DELAY_SECONDS)
            if count % 100 == 0:
                print(f"--- Sent {count} measurements ---")
            if count >= MAX_ROWS:
                print(f"--- Reached limit of {MAX_ROWS} measurements ---")
                break

if __name__ == "__main__":
    main()