import math
import random
import string

from mimesis import Datetime

datetime = Datetime('en')


def calculate_distance_between_points(point1, point2):
    """
    Calculates the distance between two gps points in meters.

    :param point1: the first point given by [lon1, lat1]
    :param point2: the second point given by [lon2, lat2]
    :return: the distance in meters
    """
    earth_radius = 6378.137

    diff_lat = math.radians(point2[1] - point1[1])
    diff_lon = math.radians(point2[0] - point1[0])
    a = math.sin(diff_lat / 2) * math.sin(diff_lat / 2) + math.sin(diff_lon / 2) * math.sin(diff_lon / 2) * math.cos(
        math.radians(point1[1])) * math.cos(math.radians(point2[1]))
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

    return earth_radius * c * 1000


def shift(point, bearing, distance):
    """
    Shifts a given gps point by a given angle and a given distance.
    :param point: the gps point given by [lon1, lat1]
    :param bearing: the angle given in degrees
    :param distance: the distance in km
    :return: the shifted gps point
    """
    earth_radius = 6378.137
    bearing = math.radians(bearing)
    lon1 = math.radians(point[0])
    lat1 = math.radians(point[1])
    lat2 = math.asin(math.sin(lat1) * math.cos(distance / earth_radius) + math.cos(lat1) * math.sin(
        distance / earth_radius) * math.cos(bearing))
    lon2 = lon1 + math.atan2(math.sin(bearing) * math.sin(distance / earth_radius) * math.cos(lat1),
                             math.cos(distance / earth_radius) - math.sin(lat1) * math.sin(lat2))
    # the 2 lines below have to be separated from the lines above
    # otherwise the method is not working properly due to lazy evaluation
    lat2 = math.degrees(lat2)
    lon2 = math.degrees(lon2)
    return [lon2, lat2]


def find_closest_charging(curr_point, stations):
    min_dist = calculate_distance_between_points(curr_point, stations[0])
    closest = stations[0]
    for point in stations:
        dist = calculate_distance_between_points(curr_point, point)

        if dist < min_dist:
            min_dist = dist
            closest = point
    return closest


def id_generator(size=18, chars=string.ascii_lowercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))


def create_status(vehicle_id, trip_id,  curr, distance, battery, timestamp):
    status_id = id_generator()
    return {
        'key': status_id,
        'value': {
            'statusId': status_id,
            'tripId': trip_id,
            'vehicleId': vehicle_id,
            'position': {
                'lat': curr[0],
                'lon': curr[1]
            },
            'batteryLevel': battery,
            'distance': distance,
            'timestamp': str(timestamp)
        }
    }
