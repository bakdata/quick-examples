import json
import os
import random

import jsonlines
from mimesis import Transport

import car_sharing_simulator.simulator
from car_sharing_simulator import current_path
from car_sharing_simulator.help_functions import id_generator

DATA_VEHICLES_JSON = os.getenv('DEMO_VEHICLE_PATH') or f'{current_path}/../data/vehicles.json'

transport = Transport('en')


def create_vehicle(id):
    return {
        'key': id,
        'value': {
            'id': id,
            'name': transport.car(),
            'maxRange': random.randint(100, 400)
        }
    }


if __name__ == "__main__":
    # if a status.json exists, we require matching vehicle. Otherwise, we simply generate data with random ids
    if os.path.exists(car_sharing_simulator.simulator.JSON):
        print("Found existing status data. Generate matching data...")
        with jsonlines.open(car_sharing_simulator.simulator.JSON) as reader:
            ids = set()
            for obj in reader:
                ids.add(obj["value"]["vehicleId"])
        id_generator_ = (x for x in ids)
    else:
        id_generator_ = (id_generator(6) for _ in range(500))

    vehicle_data = [create_vehicle(id) for id in id_generator_]
    print(f"Generated {len(vehicle_data)} vehicles; Write to file...")
    with open(DATA_VEHICLES_JSON, 'w+') as vehicle_file:
        json.dump(vehicle_data, vehicle_file, indent=4)
