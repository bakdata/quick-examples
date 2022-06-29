import json
from datetime import timedelta

from mimesis import Person

from car_sharing_simulator import current_path
from car_sharing_simulator.help_functions import *
from car_sharing_simulator.trip import Trip

global list_destination
charging_points = []

with open(f"{current_path}/../data/destinations.json") as json_file:
    list_destination = json.load(json_file)

with open(f"{current_path}/../data/charging_stations.json") as json_file:
    data = json.load(json_file)
    for d in data:
        charging_points.append(d['coordinates'])


class Vehicle:
    """
    This is a vehicle class for car_sharing.
    Attributes
    ----------
    name : str
        name of the vehicle
    max_range : int
        maximum range a vehicle can drive in Kilometer
    position : dict
        current location of the vehicle
    timestamp : int
        starting timestamp of a vehicle
    """

    def __init__(self, vehicle_id, name, max_range, position, timestamp):
        Person('en')
        self.id = vehicle_id
        self.name = name
        self.max_range = max_range
        self.position = position
        self.list_destinations = list_destination
        self.charging_points = charging_points
        self.curr_trip = None
        self.__dist = 0
        self.__battery = 100
        self.set_battery(random.randint(50, 100))
        self.past_trips = []
        self.timestamp = timestamp
        self.start_new_trip()

    def __str__(self):
        return '{id: ' + str(self.id) + ', name: ' + str(self.name) + ', max_range: ' + str(
            self.max_range) + ', position: ' + str(self.position) + ', curr_trip: ' + str(self.curr_trip) + '}'

    def start_new_trip(self):
        if not self.is_on_trip():
            # if last segment is the charging station
            if self.position in self.charging_points:
                self.set_dist(0)

            if self.get_battery() < 30:
                self.curr_trip = Trip(self.position, find_closest_charging(self.position, self.charging_points), self)
                print('Vehicle ' + str(self.name) + ' is going to closest charging station.')
                print(f"battery status: {self.get_battery()}")
            else:
                self.curr_trip = Trip(self.position, random.choice(self.list_destinations), self)

            self.past_trips.append(self.curr_trip)

    def is_on_trip(self):
        return self.curr_trip and self.curr_trip.end != self.position

    def drive(self, seconds=5, trips=20):
        step_index = self.curr_trip.steps.index(self.position)

        duration_to_drive = self.curr_trip.driven_duration + seconds
        sum_duration_point = round(sum(self.curr_trip.duration_list[0:step_index]), 2)
        new_step_index = step_index
        while len(self.curr_trip.duration_list) > new_step_index and \
                sum_duration_point + self.curr_trip.duration_list[new_step_index] < duration_to_drive:
            sum_duration_point = sum_duration_point + self.curr_trip.duration_list[new_step_index]
            new_step_index = new_step_index + 1

        route = self.curr_trip.steps[step_index:new_step_index + 1]  # take next segments from array

        segments = []
        # extract segments from route
        for index, elem in enumerate(route):
            if index + 1 < len(route):  # Check index bounds
                dist = calculate_distance_between_points(elem, route[index + 1])
                self.set_dist(self.get_dist() + dist)
                self.timestamp = self.timestamp + timedelta(
                    seconds=round(self.curr_trip.duration_list[step_index + index], 2))

                # use function from help_functions.py
                status_data = create_status(self.id, self.curr_trip.id, elem, self.get_dist(),
                                            int(self.get_battery()), int(self.timestamp.timestamp()))
                segments.append(status_data)

                # change curr position
        self.curr_trip.driven_duration = duration_to_drive
        self.position = route[-1]
        # you can skip this if you don't define a num of trips in status_generator.py and stop the script manually
        if len(self.past_trips) < trips:
            self.start_new_trip()
        return segments

    def get_dist(self):
        return self.__dist

    def set_dist(self, dist):
        self.__dist = dist
        distance_driven = float(dist)  # update battery status with driven dist
        max_range_meters = float(self.max_range * 1000)
        if distance_driven > max_range_meters:
            self.__battery = 0
        else:
            self.__battery = round((max_range_meters - distance_driven) / max_range_meters * 100)

    def set_battery(self, battery):
        if 100 >= battery >= 0:
            self.__battery = battery
            self.__dist = (battery / 100) * self.max_range * 1000

    def get_battery(self):
        return self.__battery
