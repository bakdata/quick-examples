import os

import jsonlines
import requests
from ratelimit import limits, sleep_and_retry

from car_sharing_simulator import current_path

JSON = f'{current_path}/../data/status.jsonl'

API_KEY = os.getenv("QUICK_API_KEY")
QUICK_URL = os.getenv("QUICK_URL")
TOPIC = os.getenv("QUICK_STATUS_TOPIC" or "status")
NUM_UPDATES = int(os.getenv("QUICK_NUM_UPDATES") or 50)


def __ingest_data():
    session = requests.Session()
    session.headers.update({'X-API-Key': API_KEY})
    with jsonlines.open(JSON) as reader:
        updates = []
        for obj in reader:
            obj["value"]["timestamp"] = int(obj["value"]["timestamp"])
            updates.append(obj)
            if len(updates) == NUM_UPDATES:
                send_data(updates, session)
                updates = []


@sleep_and_retry
@limits(calls=5, period=1)
def send_data(updates, session):
    response = session.post(f'{QUICK_URL}/ingest/{TOPIC}', json=updates)
    if response.status_code != 200:
        print(f"Error occurred during ingestion {response.content}")


if __name__ == "__main__":
    print("Starting to ingest data")
    # Ingest data for ever...
    while True:
        __ingest_data()
