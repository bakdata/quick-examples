import json
import os
import random

from mimesis import Transport

from car_sharing_simulator import current_path

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
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("-n", "--num-vehicles", type=int, default=500, help="Generate n vehicles.")
    args = parser.parse_args()

    vehicle_data = [create_vehicle(str(id)) for id in range(args.num_vehicles)]
    print(f"Generated {len(vehicle_data)} vehicles; Write to file...")
    with open(DATA_VEHICLES_JSON, 'w+') as vehicle_file:
        json.dump(vehicle_data, vehicle_file, indent=2)
