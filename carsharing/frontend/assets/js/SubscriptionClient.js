const GQL_CONNECTION_INIT = 'connection_init'
const GQL_START = 'start'
const GQL_CONNECTION_ACK = 'connection_ack'
const GQL_DATA = 'data'
const GQL_ERROR = 'error'

export default class SubscriptionClient {
    constructor(host, gateway, query) {
        this.baseUrl = host.replace(/https?:\/\//, '');
        this.gateway = gateway
        this.query = query
    }

    receive(ack_callback, data_callback, err_callback) {
        console.log("receive function called")
        return message => {
            message = JSON.parse(message['data']);
            if (message['type'] === GQL_CONNECTION_ACK) {
                console.log("ack_callback")
                return ack_callback();
            }

            if (message['type'] === GQL_DATA) {
                // console.log("data_callback")
                return data_callback(message["payload"]["data"])
            }

            if (message['type'] === GQL_ERROR) {
                console.log("Error", message)
                return err_callback(message["payload"]["errors"]);
            }
        }
    }

    subscribe(ack_callback, data_callback, err_callback) {
        let websocket = new WebSocket(`wss://${this.baseUrl}/gateway/${this.gateway}/graphql-ws`);
        let payload = {'type': GQL_CONNECTION_INIT}
        let query = {'id': 1, 'type': GQL_START, 'payload': {'query': this.query}};
        console.log("opening websocket")
        websocket.onopen = () => {
            websocket.send(JSON.stringify(payload));
            websocket.send(JSON.stringify(query));
        }
        websocket.onmessage = this.receive(ack_callback, data_callback, err_callback);
    }
}
