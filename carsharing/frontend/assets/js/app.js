import SubscriptionClient from "./SubscriptionClient.js";
import util from "./util.js";
import {ICONS} from "./icons.js";

let trips = []
let validIndex = 0
let currentTime = 0;
let isHovering = false;

const ANIMATION_SPEED = 3;
const startTime = new Date().getTime();

const {DeckGL, IconLayer, TripsLayer, PolygonLayer, MapView} = deck;

const ICON_MAPPING = {marker: {x: 0, y: 0, width: 512, height: 512, anchorY: 512, mask: false}};

const ICON_LAYER = new IconLayer({
    id: 'icon-layer',
    data: ICONS,
    getPosition: d => d.coordinates,
    /*
    Font Awesome Free 5.15.0 by @fontawesome - https://fontawesome.com
    License - https://fontawesome.com/license/free (Icons: CC BY 4.0, Fonts: SIL OFL 1.1, Code: MIT License)
     */
    iconAtlas: './assets/images/charging.png',
    iconMapping: ICON_MAPPING,
    getIcon: d => 'marker',
    sizeUnits: 'pixels',
    sizeScale: 50,
    sizeMinPixels: 0
});


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

const DECK_GL = new DeckGL({
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
    onHover: ({object}) => (isHovering = Boolean(object)),
    getCursor: ({isDragging}) => (isDragging ? 'grabbing' : (isHovering ? 'pointer' : 'grab'))
});

function getTooltip({object}) {
    if (!object) {
        return
    }

    let timestampNow = Math.floor(currentTime + parseInt(object.firstTimestamp))
    let index = object.timestamps.indexOf(timestampNow.toString())
    if (index !== -1) {
        validIndex = index
    }
    let time = new Date(object.timestamps[validIndex] * 1000).toLocaleTimeString()
    let battery = object.batteries[validIndex]
    return {
        html: `
           <h6>Trip ID: ${object.tripId}</h6>
           <p>VehicleID: ${object.vehicleId}</p>
           <p>Battery Level: ${battery}%</p>
           <p>Distance Driven: ${object.distance / 1000} Km</p>
           <p>Time: ${time}</p>
`,
        style: {
            backgroundColor: '#434C5E',
            fontSize: '0.8em'
        }
    }
}

// This functions calls the subscription client and sets the function references
function prepareSubscription(query) {
    console.log("prepare subscription")
    const host = "demo.d9p.io";
    const gateway = "car-sharing";
    let client = new SubscriptionClient(host, gateway, query)
    client.subscribe(ackCallback, subscribeCallback, errorCallback)
}

// Callbacks
function ackCallback() {
    console.log("ack_callback!")
}

function errorCallback(error) {
    console.log("Error!" + error)
}

// whenever the data is ready this function is called
function subscribeCallback(payload) {
    const data = payload["carSharing"];
    let trip = trips[data["tripId"]]
    if (trip === undefined) {

        // initialize first time we see this trip
        trips[data["tripId"]] = {
            "vehicleId": data["vehicleId"],
            "tripId": data["tripId"],
            "path": [],
            "timestamps": [],
            "batteryLevel": 0,
            "batteries": [],
            "distance": 0,
            "firstTimestamp": Number.MAX_VALUE,
        }

        trip = trips[data["tripId"]]
    }
    if (trip["firstTimestamp"] > data["timestamp"]) {
        trip["firstTimestamp"] = data["timestamp"]
    }
    // find correct position where we have to insert elements based on timestamp
    let index = util.locationOf(data["timestamp"], trip["timestamps"])
    util.insert([data["position"]["lat"], data["position"]["lon"]], index, trip["path"])
    util.insert(data["timestamp"], index, trip["timestamps"])
    util.insert(data["batteryLevel"], index, trip["batteries"])
    trip["batteryLevel"] = data["batteryLevel"]
    trip["distance"] = data["distance"]
}


function animate() {
    const now = new Date().getTime();
    currentTime = (((now - startTime) / 1000) * ANIMATION_SPEED) % 2000;
    window.requestAnimationFrame(animate);
}

// This function draws the upcoming data on the map
function render() {
    let values = Object.keys(trips).map(function (key) {
        return trips[key];
    });

    const TRIPS_LAYER = new TripsLayer({
        id: 'trips',
        data: values,
        getPath: d => d.path,
        getColor: d => d.batteries.map((b) => util.getGreenToRed(b)),
        opacity: 1,
        widthMinPixels: 4,
        rounded: true,
        trailLength: 100,
        getTimestamps: d => d.timestamps.map(t => t - d.firstTimestamp),
        currentTime: currentTime,
        pickable: true,
        onClick: (info, event) => util.openInNewTab(`./query.html?tripId=${info.object.tripId}`),

    });
    DECK_GL.setProps({layers: [TRIPS_LAYER, ICON_LAYER, POLYGON_LAYER], getTooltip});
    requestAnimationFrame(render)
}

// Read the query and create websocket connections
const query = `
subscription {
    carSharing {
        statusId
        tripId
        vehicleId
        position {
            lat
            lon
        }
        batteryLevel
        distance
        timestamp
    }
}
`
prepareSubscription(query)
requestAnimationFrame(animate)
requestAnimationFrame(render)
