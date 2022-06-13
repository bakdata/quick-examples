import json
import urllib.request
from urllib.error import URLError

from car_sharing_simulator.help_functions import id_generator


class Trip:
    def __init__(self, start, end, vehicle):
        self.start = start
        self.end = end
        self.vehicle = vehicle
        self.id = id_generator(size=6)
        # request route from osrm api
        coordinates = f"{self.start[0]},{self.start[1]};{self.end[0]},{self.end[1]}"
        # http://localhost:5000/route/v1/driving/13.307689733356195,52.49550979090535;13.460582507417463,52.51022326723362?steps=true&geometries=geojson&overview=full&annotations=duration
        link = f"http://localhost:5000/route/v1/driving/{coordinates}?steps=true&geometries=geojson&overview=full&annotations=duration"
        try:
            res = urllib.request.urlopen(link)
            json_obj = json.loads(res.read())
            # extract distance and steps from json
            self.distance = (json_obj['routes'][0]['distance'])  # in meters
            self.steps = json_obj['routes'][0]['geometry']['coordinates']
            self.steps.insert(0, self.start)  # add start coordinates
            self.steps.append(self.end)  # add end coordinates
            self.driven_duration = 0
            self.duration_list = json_obj['routes'][0]['legs'][0]['annotation']['duration']
            self.duration_list.insert(0, 0)
            self.duration_list.append(0)

        except URLError as e:
            print('URLError = ' + str(e.reason))

    def __str__(self):
        return '{id: ' + str(self.id) + ', start: ' + str(self.start) + ', end: ' + str(self.end) + '}'
