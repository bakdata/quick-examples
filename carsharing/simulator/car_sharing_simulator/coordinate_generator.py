import json
from random import uniform
import requests

from car_sharing_simulator import current_path
from car_sharing_simulator.help_functions import shift


def generate(num_of_points, charging_station):
    locations = []
    bearing = 0
    for i in range(num_of_points):
        distance = uniform(0, 8)
        coordinates = shift(point=[13.404805144011382, 52.520062555274194], bearing=bearing, distance=distance)
        link = f"http://localhost:5000/nearest/v1/driving/{coordinates[0]},{coordinates[1]}?number=1"
        response = requests.get(link).json()
        point = response["waypoints"][0]["location"]
        if charging_station:
            point = {"name": f"charging{i + 1}", "coordinates": point}
        locations.append(point)
        bearing = bearing + (360 / num_of_points)
        print(bearing)
    file_name = 'charging_stations.json' if charging_station else 'destinations.json'
    with open(f"{current_path}/../data/{file_name}", 'w') as outfile:
        json.dump(locations, outfile, indent=2)
    print(json.dumps(locations))


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("-d", "--destination", default="random", choices=["random", "charging"],
                        help="Generate random destinations or charging station locations.")
    parser.add_argument("-n", "--num-points", type=int, default=60, help="Number of points to generate.")
    args = parser.parse_args()
    generate(num_of_points=args.num_points, charging_station=args.destination == "charging")
