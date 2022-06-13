import json
import random
from datetime import datetime

import jsonlines

from car_sharing_simulator import current_path
from car_sharing_simulator.vehicle import Vehicle, charging_points


def start():
    now = datetime.now()  # use the current time to start
    number_vehicles = 100  # number of vehicles
    update_time = 20  # number of seconds pro drive update
    trips_per_vehicle = 51  # number of trips pro vehicle to travel
    vehicles = []

    # generate all vehicles
    with open(f'{current_path}/../data/vehicles.json') as v_file:
        vehicle_list = json.load(v_file)

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

    print(f"Calculating {trips_per_vehicle} trips for {number_vehicles}")
    while driving:
        driving = False
        for vehicle in vehicles:
            if len(vehicle.past_trips) < trips_per_vehicle:  # check if there are more trips to be travelled
                driving = True
                new_data = vehicle.drive(update_time, trips_per_vehicle)
                data.extend(new_data)

    print("Writing data to file. This might take a while...")
    with jsonlines.open(f'{current_path}/../data/status.jsonl', mode='w+') as writer:
        writer.write(data)


if __name__ == "__main__":
    start()
