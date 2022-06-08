import json
from typing import List, Dict
import logging

import gql
from dash_extensions import WebSocket
from gql.client import Client
from gql.transport.requests import RequestsHTTPTransport

from listen_data import ListeningEvent


def make_header(api_key: str = None) -> Dict:
    header = {"content-type": "application/json"}
    if api_key:
        header["X-API-Key"] = api_key
    return header


class GatewayClient:
    def __init__(
        self,
        quick_host: str,
        gateway_name: str,
        api_key: str = None,
    ):
        self.quick_host = quick_host
        self.gateway_name = gateway_name
        self.api_key = api_key
        self.logger = logging.getLogger(self.__class__.__name__)

    def new_client(self):
        transport = RequestsHTTPTransport(
            f"https://{self.quick_host}/gateway/{self.gateway_name}/graphql",
            headers=make_header(self.api_key),
        )
        return Client(transport=transport, fetch_schema_from_transport=True)

    def new_subscription_socket(self):
        initial_msg = {
            "type": "connection_init",
            "payload": make_header(self.api_key),
        }
        return WebSocket(
            url=f"wss://{self.quick_host}/gateway/{self.gateway_name}/graphql-ws",
            id="listening_events_update",
            send=initial_msg,
        )

    @staticmethod
    def subscription_request(session_id: int):
        # Follows https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
        return json.dumps(
            {
                "type": "start",
                "payload": {
                    "query": """
                     subscription {
                         listeningEventUpdate {
                            userId
                            artist { name }
                            album { name }
                            track { name }
                            timestamp
                        }
                     }
                     """
                },
                "id": session_id,
            }
        )

    def send_listeningevent(self, event: ListeningEvent):
        query = gql.gql(
            """
        mutation SubmitListeningEvent(
            $user_id: Long!,
            $artist_id: Long!,
            $album_id: Long!,
            $track_id: Long!,
            $timestamp: Long!
        ) {
            sendListeningEvent(
                userId: $user_id,
                event: {
                    userId: $user_id,
                    artistId: $artist_id,
                    albumId: $album_id,
                    trackId: $track_id,
                    timestamp: $timestamp
                }
            ) { userId }
        }
        """
        )
        try:
            response = self.new_client().execute(
                query,
                variable_values=dict(
                    user_id=event.user_id,
                    artist_id=event.artist_id,
                    album_id=event.album_id,
                    track_id=event.track_id,
                    timestamp=event.timestamp,
                ),
            )
            self.logger.debug(str(response))
            self.logger.debug(
                f"Submitted listeningevent for user {response['sendListeningEvent']['userId']}"
            )
        except Exception as e:
            self.logger.debug(f"Error submitting listentingevent: {str(e)}")

    def has_listened(self, user_id: int) -> False:
        query = gql.gql(
            """
            query getListenCount($user_id: Long!) {
                getUserProfile(userId: $user_id) {
                    totalListenCount
                }
            }
            """
        )
        try:
            response = self.new_client().execute(
                query, variable_values={"user_id": user_id}
            )["getUserProfile"]["totalListenCount"]
        except Exception:
            return False
        return bool(response)

    def get_charts(self, user_id: int, kind) -> List[Dict]:
        query = gql.gql(
            f"""
            query getSpecificCharts($user_id: Long!) {{
                getUserProfile(userId: $user_id) {{
                    {kind}Charts {{
                        topK {{
                            countPlays
                            {kind} {{
                                id
                                name
                            }}
                        }}
                    }}
                }}
            }}
            """
        )
        try:
            response = self.new_client().execute(
                query, variable_values={"user_id": user_id}
            )
            # flatten the results
            return [
                {
                    "countPlays": d["countPlays"],
                    "name": d[kind]["name"],
                    "id": d[kind]["id"],
                }
                for d in response["getUserProfile"][f"{kind}Charts"]["topK"]
            ]
        except Exception as e:
            self.logger.error("Error querying gateway: " + str(e))
            return []

    def get_recommendations(self, user_id: int) -> List[str]:
        query = gql.gql(
            """
            query getArtistRecommendations($user_id: Long!) {
                getArtistRecommendations(userId: $user_id, field: ARTIST) {
                    recommendations {
                        artist {
                            name
                        }
                    }
                }
            }
            """
        )
        try:
            response = self.new_client().execute(
                query, variable_values={"user_id": user_id}
            )["getArtistRecommendations"]
            return [a["artist"]["name"] for a in response["recommendations"]]
        except Exception as e:
            self.logger.error("Error querying gateway: " + str(e))
            return []
