const {DeckGL, PolygonLayer, MapView, PathLayer} = deck;

const tripQuery = `
query trip($id: String) {
    trip(id: $id) {
        id
        vehicle {
          name
          maxRange
        }
        route {
            statusId
            position {
                lat
                lon
            }
            statusId
            distance
            timestamp
        }
    }
}
`

function insertCell(id, field) {
    const row = document.getElementById(id);
    const newCell = row.insertCell(-1);
    const newText = document.createTextNode(field);
    newCell.appendChild(newText);
}

function updateTable(data) {
    document.getElementById("tripid-row")
    insertCell("tripid-row", data["id"]);
    insertCell("vehicle-name-row", data["vehicle"]["name"])
    insertCell("vehicle-range-row", data["vehicle"]["maxRange"])
    let route = data["route"];
    const distance = (route[route.length - 1]["distance"]  - route[0]["distance"]) / 1000
    insertCell("distance-row", distance + "km")
    const minutes = (route[route.length - 1]["timestamp"] - route[0]["timestamp"]) / 60
    insertCell("time-row", minutes.toFixed(2) + " Minutes")
}

function getRoute() {
    const urlParams = new URLSearchParams(window.location.search);
    const tripId = urlParams.get('tripId');

    const options = {
        method: "post",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            query: tripQuery,
            variables: {"id": tripId}
        })
    };

    console.log("Fetch GraphQL: ", options);

    fetch(`https://demo.d9p.io/gateway/car-sharing/graphql`, options)
        .then(res => res.json())
        .then(initializeQuerySite)
}

function initializeQuerySite(trip) {
    const element = trip["data"]["trip"];
    updateTable(element)

    const path = element["route"].map(status => Object.values(status["position"]));
    const data = [{
        "tripId": element["tripId"],
        "vehicleId": element["vehicleId"],
        "path": path
    }]

    const POLYGON_LAYER = new PolygonLayer({
        id: 'PolygonLayer',
        data: [{
            "polygon": [[-180.0, 85.0],
                [180.0, 85.0],
                [180.0, -85.0],
                [-180.0, -85.0]]
        }],

        /* props from PolygonLayer class */
        extruded: true,
        filled: true,
        getElevation: 0,
        getFillColor: [83, 112, 180, 70],
        getLineColor: [76, 86, 106, 30],
        lineWidthMinPixels: 1,
        stroked: true,
        wireframe: true,
        pickable: false,
    });


    const PATH_LAYER = new PathLayer({
        id: 'path-layer',
        data,
        pickable: true,
        getPath: d => d.path,
        getColor: [0, 255, 0, 200],
        getWidth: 8
    });

    new DeckGL({
        container: "deckgl-map",
        views: new MapView({
            repeat: false,
        }),
        mapStyle: 'https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json',
        initialViewState: {
            longitude: 13.404954,
            latitude: 52.5200066,
            zoom: 12,
            minZoom: 11,
            pitch: 40,
            bearing: 0
        },
        controller: true,
        layers: [PATH_LAYER, POLYGON_LAYER]
    });

}

getRoute()