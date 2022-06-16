# Quick Carsharing Simulator

## OSRM backend

The simulator is based on [OSRM](http://project-osrm.org/).
You can host the OSRM backend locally.
This requires a setup as described [here](https://github.com/Project-OSRM/osrm-backend).
For Berlin map data, execute the following:

```shell
mkdir osrm && cd osrm
wget http://download.geofabrik.de/europe/germany/berlin-latest.osm.pbf
docker run -t -v "${PWD}:/data" osrm/osrm-backend:v5.22.0 osrm-extract -p /opt/car.lua /data/berlin-latest.osm.pbf
docker run -t -v "${PWD}:/data" osrm/osrm-backend:v5.22.0 osrm-contract /data/berlin-latest.osrm
```
Now you can host the api with `docker-compose`.

## Generate vehicles and status

In the first step, we generate the data and write it to files:

```shell
cd ./simulator
mkdir ./data
python -m car_sharing_simulator.vehicles_generator
python -m car_sharing_simulator.coordinate_generator
python -m car_sharing_simulator.coordinate_generator -d charging
python -m car_sharing_simulator.status_generator
```
For generating files with coordinates the `coordinate_generator.py` is used.
With option `-d charging`, the coordinates are generated for charging stations.
Otherwise, random locations are generated.

There should be two files named `vehicles.json` and `status.jsonl` in the `./data/` directory.
Start with ingesting all vehicles:

```
curl -X POST --url $QUICK_URL/ingest/vehicle \
    --header 'content-type: application/json' \
    --header 'X-API-Key:<API-KEY>' \
    --data "@./data/vehicles.json"
```

Ingestion of new status events can be started with `python -m car_sharing_simulator.simulator`.
The script expects the following environment variables:

| Name               | Example                     | Required          |
|--------------------|-----------------------------|-------------------|
| QUICK_API_KEY      | W9tK8edVnXXa1gMyFVVh        | Yes               |         
| QUICK_URL          | https://demo.d9p.io/ingest  | Yes               |
| QUICK_STATUS_TOPIC | status                      | Yes               |
| QUICK_NUM_UPDATES  | 200                         | No (default: 200) |




