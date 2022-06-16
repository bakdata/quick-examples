import json
import random
import warnings
from datetime import datetime

import jsonlines

from car_sharing_simulator import current_path
from car_sharing_simulator.vehicle import Vehicle, charging_points


def start(update_time, trips_per_vehicle, number_vehicles=None):
    now = datetime.now()  # use the current time to start
    vehicles = []

    # generate all vehicles
    with open(f'{current_path}/../data/vehicles.json') as v_file:
        vehicle_list = json.load(v_file)

    if number_vehicles:
        if number_vehicles > len(vehicle_list):
            warnings.warn(f"Found only {len(vehicle_list)} vehicles.")
            number_vehicles = len(vehicle_list)
    else:
        number_vehicles = len(vehicle_list)

    for i in range(number_vehicles):
        vehicles.append(Vehicle(
            vehicle_list[i]["value"]["id"],
            vehicle_list[i]["value"]["name"],
            vehicle_list[i]["value"]["maxRange"],
            random.choice(charging_points),
            now
            # use timestamp if you want to set your own date and time
        ))
    data = []
    driving = True

    print(f"Calculating {trips_per_vehicle} trips for {number_vehicles} vehicles.")
    while driving:
        driving = False
        for vehicle in vehicles:
            if len(vehicle.past_trips) < trips_per_vehicle:  # check if there are more trips to be travelled
                driving = True
                new_data = vehicle.drive(update_time, trips_per_vehicle)
                data.extend(new_data)

    print("Writing data to file. This might take a while...")
    with jsonlines.open(f'{current_path}/../data/status.jsonl', mode='w') as writer:
        writer.write_all(data)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("-u", "--update-time", type=int, default=20, help="Status update time in seconds.")
    parser.add_argument("-t", "--num-trips", type=int, default=50, help="Number of trips per vehicle.")
    parser.add_argument("-n", "--num-vehicles", type=int, help="Generate status for n vehicles.")
    args = parser.parse_args()
    start(args.update_time, args.num_trips, args.num_vehicles)
