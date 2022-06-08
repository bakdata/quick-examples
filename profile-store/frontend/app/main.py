import logging
import warnings
from datetime import datetime
import json
import uuid
from argparse import ArgumentParser
from typing import List, Optional
import sys

import dash
import dash_bootstrap_components as dbc
from dash import Dash, html, Output, Input, State, MATCH, ALL
from dash.development.base_component import Component
from dash.exceptions import PreventUpdate
from dash_extensions import websockets
from dash_extensions.enrich import ServersideOutput

from app.client import GatewayClient
from app.generate_users import create_avatar
from app.listen_data import ListenData, ListeningEvent
from app.users import make_user
from app.utils import (
    timestamp2last_active_status,
    timestamp2datetime,
    format_song,
    button_style,
    Colors,
    new_ticker,
)

D9P_STYLES = "https://d9p.io/wp-content/themes/wp_streamnatives_quick/A.style.css,qver=5.8.2.pagespeed.cf.vGx7YlD7RD.css"
app = Dash("quick-profile", external_stylesheets=[dbc.themes.BOOTSTRAP, D9P_STYLES])
app.title = "Quick FM"

# this is global state, which is not recommended by dash when using with multiple workers
# however, via the seed parameter it is controlled that all instances behave deterministically
data: Optional[ListenData] = None
# gateway has no randomness anyway
gateway: Optional[GatewayClient] = None


def make_header():
    return html.Header(
        className="site-header",
        children=html.Div(
            className="site-branding",
            children=html.P(
                className="site-title",
                children=html.A(
                    className="mainlogo logo",
                    children=[
                        html.Span("datanonstop ", className="logo__mainbrand"),
                        html.Span("|", className="logo__separator"),
                        html.Span(" quick ", className="logo__subbrand"),
                    ],
                    href="https://d9p.io",
                    rel="home",
                ),
            ),
        ),
    )


def main_layout(
    client_user: Component,
    user_radio: Component,
    users: List[Component],
    user_details: Component,
    functionals: List[Component],
):
    """
    Put everything together

    :param client_user: The component for the app user
    :param user_radio: The component for selecting tracks
    :param users: A list of components for the demo data users
    :param user_details: A container to show detailed information (charts & recommendations)
    :param functionals: Invisible components used in the callbacks
    :return: the app layout
    """
    session_id = str(uuid.uuid4())
    return html.Div(
        id={"type": "session", "id": session_id},
        children=[
            make_header(),
            html.Div(
                className="site-content",
                id="content",
                children=html.Div(
                    className="app-container",
                    children=[
                        html.Div(
                            className="app",
                            children=[
                                html.Div(
                                    [client_user, user_radio, *users],
                                    style=dict(
                                        display="flex",
                                        flexFlow="row wrap",
                                        justifyContent="flex-start",
                                        width="70%",
                                    ),
                                    id="user_queue",
                                ),
                                html.Div(
                                    user_details,
                                    style=dict(
                                        width="30%",
                                        position="sticky",
                                        top="80px",
                                        alignSelf="flex-start",
                                        display="flex",
                                        flexFlow="column nowrap",
                                        justifyContent="center",
                                    ),
                                ),
                            ],
                            style=dict(
                                display="flex",
                                flexFlow="row nowrap",
                                width="100%",
                            ),
                        ),
                        html.Div(
                            functionals,
                            className="functionals",
                            style=dict(display="none"),
                        ),
                    ],
                    style=dict(top="0px", position="relative"),
                ),
            ),
        ],
    )


################################
# Creating the main Components #
################################


def make_client_user() -> Component:
    """
    The special user for the page visitor
    """
    # we want a fresh id for each session
    id_ = hash(datetime.now())
    while gateway.has_listened(id_):
        id_ = hash(datetime.now())
    return make_user(
        id_,
        html.I(f"You!"),
        create_avatar("nb"),
        order=0,
        updatable=True,
        user_type="custom",
    )


def make_user_radio() -> Component:
    """
    The song selection widget
    """
    hw = dict(minHeight="50px", minWidth="100px", fontSize="xx-large", marign="15px")
    return html.Div(
        [
            html.H3("Select a song"),
            html.H2("\U0001f3a7", style=dict(fontSize="100px")),
            html.Div(
                id={"type": "current_select", "track": -1, "album": -1, "artist": -1},
                style=dict(
                    margin="5px",
                    borderRadius="8px",
                    width="100%",
                    display="flex",
                    flexFlow="column wrap",
                    alignItems="center",
                    backgroundColor=Colors.base,
                ),
            ),
            html.Div(
                [
                    html.Button(
                        "\u23ee",  # U+23EE
                        id="user_radio_prev",
                        style={**button_style, **hw},
                    ),
                    html.Button(
                        "\u25b6",
                        id="user_radio_play",
                        style={**button_style, **hw},
                    ),
                    html.Button(
                        "\u23ed",  # U+23ED
                        id="user_radio_next",
                        style={**button_style, **hw},
                    ),
                ],
                style=dict(width="100%", display="flex", flexFlow="row nowrap"),
            ),
        ],
        style=dict(
            display="flex",
            flexFlow="column nowrap",
            justifyContent="space-between",
            alignItems="center",
            margin="15px",
            padding="10px",
            height="auto",
            width="380px",  # sync with user_card to fit in grid
            borderRadius="8px",
            order=1,
        ),
    )


def make_demo_users() -> List[Component]:
    return [
        make_user(*user, order=pos + 2, user_type="demo")
        for pos, user in enumerate(data.users.reset_index().to_records(index=False))
    ]


def make_user_details_pane() -> Component:
    return html.Div(
        html.H3("Charts / Recommended", style=dict(textAlign="center")),
        id="detailed_pane",
    )


###################
# Re-used methods #
###################


def make_single_charts_list(user_id: int, kind: str) -> Component:
    """
    Retrieve the specific charts for a user and put them in a list component

    :param user_id: user id
    :param kind: ['artist', 'album', 'track']
    :return: the charts component
    """
    data_ = gateway.get_charts(user_id, kind)
    items = []
    for d in data_:
        if kind == "artist":
            items.append(
                html.Div(f"{d['name'] or ListenData.UNNAMED} (x {d['countPlays']})")
            )
        else:
            artist = data.get_associated_artist(d["id"], kind) or ListenData.UNNAMED
            items.append(
                html.Div(
                    [
                        *format_song(d["name"] or ListenData.UNNAMED, artist=artist),
                        f" (x{d['countPlays']})",
                    ],
                    style=dict(
                        display="flex",
                        flexFlow="row wrap",
                        whiteSpace="pre",
                        justifyContent="center",
                    ),
                )
            )
    return html.Div(
        items, style=dict(display="flex", flexFlow="column nowrap", textAlign="center")
    )


def make_charts(user_id: int) -> Component:
    """
    Create the component that displays current charts for a user
    """
    # first, only load artist data
    data_ = make_single_charts_list(user_id, kind="artist")

    def make_single_charts(user_id: int, kind: str, data_=None) -> Component:
        """
        kind: artist, album, track
        """
        # we use this function to make the example items to avoid code duplication
        return dbc.Card(
            [
                dbc.CardHeader(
                    dbc.Button(
                        f"{kind[0].upper() + kind[1:]}s",
                        id={"type": f"charts_button", "id": user_id, "kind": kind},
                        style=dict(
                            height="100%",
                            width="100%",
                            margin="3px 0px",
                            border="0px",
                            borderRadius="0px",
                            color="black",
                            backgroundColor=Colors.button,
                        ),
                    ),
                    style=dict(
                        padding="0px",
                        height="50px",
                    ),
                ),
                dbc.Collapse(
                    dbc.CardBody(
                        data_ or [],
                        id={"type": "charts_data", "id": user_id, "kind": kind},
                    ),
                    is_open=bool(data_),
                    id={"type": f"charts_collapse", "id": user_id, "kind": kind},
                ),
            ],
            style=dict(
                width="100%",
                border="0px",
                margin="3px 0px",
            ),
        )

    try:
        name = data.get_name(user_id, "user") + "'s"
    except KeyError:
        name = "Your"

    return html.Div(
        [
            html.H3(f"{name} favorites", style=dict(textAlign="center")),
            make_single_charts(user_id, "artist", data_),
            make_single_charts(user_id, "album"),
            make_single_charts(user_id, "track"),
        ],
        style=dict(width="auto"),
    )


def make_recommendation(user_id: int) -> Component:
    """
    Fetch the artist recommendations for a user and wrap them in a component
    """
    recommendations = gateway.get_recommendations(user_id) or [
        "Could not find any recommendations :("
    ]
    try:
        name = data.get_name(user_id, "user")
    except KeyError:
        name = "You"
    return html.Div(
        [
            html.H3(f"{name} may like", style=dict(textAlign="center")),
            html.Div(
                [html.P(r) for r in recommendations],
                style=dict(
                    marginTop="15px",
                    display="flex",
                    flexFlow="column nowrap",
                    alignItems="center",
                ),
            ),
        ],
        style=dict(
            width="100%",
        ),
    )


##################
# Dash Callbacks #
##################

# for matching
all_users = {"id": MATCH, "user_type": ALL}


@app.callback(
    Output(
        {"type": "current_select", "track": ALL, "artist": ALL, "album": ALL},
        "children",
    ),
    Output({"type": "current_select", "track": ALL, "artist": ALL, "album": ALL}, "id"),
    Input("user_radio_prev", "n_clicks"),
    Input("user_radio_next", "n_clicks"),
)
def user_radio_prev_next(prev_clicks, next_clicks):
    """
    Select previous / next song
    """
    d = data.get_track_fullinfo((next_clicks or 0) - (prev_clicks or 0))
    return (
        [format_song(d.track_name, d.artist_name, d.album_name)],
        [
            {
                "type": "user_radio",
                "track": int(d.track_id),
                "artist": int(d.artist_id),
                "album": int(d.album_id),
            }
        ],
    )


@app.callback(
    Output("dummy", "children"),
    Input("user_radio_play", "n_clicks"),
    State({"type": "current_select", "track": ALL, "artist": ALL, "album": ALL}, "id"),
    State({"type": "current_listen", "id": ALL, "user_type": "custom"}, "id"),
)
def submit_user_listen(n_clicks, ids, user_ids):
    """
    Send the currently selected song to the listeningevent stream

    needs dummy output, as no outputs are not allowed
    """

    if n_clicks:
        id_ = ids[0]
        user_id = user_ids[0]
        gateway.send_listeningevent(
            ListeningEvent(
                user_id=user_id["id"],
                artist_id=id_["artist"],
                album_id=id_["album"],
                track_id=id_["track"],
                timestamp=int(datetime.now().timestamp()) * 1000,  # in millis
            )
        )
        return ""
    raise PreventUpdate


def make_functionals() -> List[Component]:
    return [
        html.Div(id="dummy", style=dict(display="none")),
        html.Div(id="ws_message", style=dict(display="none")),
        html.Div(id="current_user_id_div", style=dict(display="none")),
        gateway.new_subscription_socket(),
    ]


@app.callback(
    ServersideOutput("ws_message", "children"),
    Output("current_user_id_div", "children"),
    Input("listening_events_update", "message"),
)
def transform_message(listeningevent):
    """
    Intermediate callback to only trigger for the user that just listened
    """
    if listeningevent:
        listeningevent = json.loads(listeningevent["data"])
        if listeningevent["type"] == "data":
            item = listeningevent["payload"]["data"]["listeningEventUpdate"]
            return (
                json.dumps(item),
                html.Div(id={"type": "current_user_id", "id": item["userId"]}),
            )
    raise PreventUpdate


@app.callback(
    Output({"type": "current_user_id", "id": ALL}, "children"),
    Input("ws_message", "children"),
)
def notify_track_update(a):
    """
    We need this intermediate step, because the dynamic cb cannot react on a change to an element,
    whose id was changed in the same cb
    """
    return [a]


@app.callback(
    Output({"type": "last_listen", "id": MATCH, "user_type": ALL}, "children"),
    Output({"type": "current_listen", "id": MATCH, "user_type": ALL}, "children"),
    Input({"type": "current_user_id", "id": MATCH}, "children"),
)
def update_user_listen(listeningevent):
    """
    Update last listen time and display incoming track
    """
    if listeningevent:
        listeningevent = json.loads(listeningevent)
        artist, album, track = [
            listeningevent[k]["name"] for k in ["artist", "album", "track"]
        ]
        ts = timestamp2datetime(listeningevent["timestamp"])
        lastlisten_str = timestamp2last_active_status(ts)
        return (
            [lastlisten_str],
            [
                new_ticker(
                    ["     +++ ", *format_song(track, artist, album), " +++     "]
                )
            ],
        )
    raise PreventUpdate


@app.callback(
    Output("listening_events_update", "send"),
    Input("listening_events_update", "state"),
    State({"type": "session", "id": ALL}, "id"),
)
def init_listenstream(ws_state, ids):
    """
    Send initial request to the websocket

    Connection will be ended by timeout

    :param ws_state: the state of the websocket component (1=OPEN, 3=CLOSED)
    :param ids: List with session's id object
    :return: the initial request
    """
    if ws_state and ws_state["readyState"] == 1:
        operation_id = ids[0]["id"]
        return gateway.subscription_request(operation_id)
    else:
        raise PreventUpdate


@app.callback(
    Output("detailed_pane", "children"),
    Output({"type": "user_card", "id": ALL, "user_type": ALL}, "style"),
    Input({"type": "charts_button", "id": ALL, "user_type": ALL}, "n_clicks"),
    Input({"type": "recommendation_button", "id": ALL, "user_type": ALL}, "n_clicks"),
    Input("listening_events_update", "message"),
    State({"type": "user_card", "id": ALL, "user_type": ALL}, "id"),
    State({"type": "user_card", "id": ALL, "user_type": ALL}, "style"),
    State("detailed_pane", "children"),
)
def display_user_details_or_change_user_order(
    _, __, ws_event, user_ids, current_styles, current_details
):
    """
    Display the details for a user and highlight their user card

    or

    On listeningEventsUpdate, change the order of users, such that the most
    recent listener is on top of queue

    :param _: charts button n_clicks
    :param __: recommendation button n_clicks
    :param user_ids: List of all user card id objects
    :param current_styles: The styles of the individual user containers (for highlighting)
    :return: the detailed Component for the user
    """
    trigger = dash.callback_context.triggered[0]["prop_id"].split(".")[0]
    if not trigger:
        raise PreventUpdate

    # change the user order on incoming listeningevent
    if trigger == "listening_events_update" and ws_event:
        ws_event = json.loads(ws_event["data"])
        if ws_event["type"] == "data":
            current_listener = ws_event["payload"]["data"]["listeningEventUpdate"][
                "userId"
            ]
            for user_style, user_id in zip(current_styles, user_ids):
                if user_id["id"] == current_listener:
                    user_order = user_style["order"]
                    break
            else:
                # the incoming listening event came from another custom user in a different session
                raise PreventUpdate
            # pos 0 and 1 are app user and user radio, so front is pos 2
            if user_order:
                for s in current_styles:
                    order = s["order"]
                    if order < 2:
                        continue
                    elif order == user_order:
                        # current listener after custom and radio
                        s["order"] = 2
                    elif order < user_order:
                        s["order"] = order + 1
            return current_details, current_styles
        else:
            raise PreventUpdate

    trigger = json.loads(trigger)
    # highlight selected user, display charts/ recommendations
    for uid, style in zip(user_ids, current_styles):
        if uid["id"] == trigger["id"]:
            style["backgroundColor"] = Colors.user_selected
        else:
            style["backgroundColor"] = Colors.base

    if trigger["type"] == "charts_button":
        return make_charts(trigger["id"]), current_styles
    elif trigger["type"] == "recommendation_button":
        return make_recommendation(trigger["id"]), current_styles
    raise PreventUpdate


@app.callback(
    Output({"type": "charts_collapse", "id": ALL, "kind": ALL}, "is_open"),
    Output({"type": "charts_data", "id": ALL, "kind": ALL}, "children"),
    Input({"type": "charts_button", "id": ALL, "kind": ALL}, "n_clicks"),
    State({"type": "charts_data", "id": ALL, "kind": ALL}, "children"),
)
def toggle_chart_view(_, all_charts_data):
    """
    Fetch and wrap the specific charts for a user

    :param _: the trigger button
    :param all_charts_data: all 3 charts, may have been fetched or not
    """
    trigger = dash.callback_context.triggered
    if not trigger:
        raise PreventUpdate
    trigger = json.loads(trigger[0]["prop_id"].split(".")[0])
    user = trigger["id"]
    kind = trigger["kind"]
    data_ = make_single_charts_list(user, kind)
    index = next(i for i, e in enumerate(["artist", "album", "track"]) if e == kind)
    ret = [False, False, False], all_charts_data
    ret[0][index] = True
    ret[1][index] = data_
    return ret


@app.callback(
    Output({"type": "user_avatar", "id": MATCH, "user_type": ALL}, "src"),
    Input({"type": "avatar_refresh", "id": MATCH, "user_type": ALL}, "n_clicks"),
)
def update_avatar(_):
    """
    Refresh an avatar after button click

    :param _: the refresh button n_clicks
    """
    trigger = dash.callback_context.triggered[0]["prop_id"].split(".")[0]
    if not trigger:
        raise PreventUpdate
    return [f"data:image/png;base64,{create_avatar()}"]


argparser = ArgumentParser()
argparser.add_argument(
    "--quickHost", help="The quick host."
)
argparser.add_argument(
    "-g", "--gateway", help="The name of the gateway."
)
argparser.add_argument(
    "--apiKey", required=False, help="Api Token for the quick instance."
)
argparser.add_argument(
    "-p", "--port", type=int, default="8050", help="The server port of the app"
)
argparser.add_argument(
    "--debug", type=bool, default=False, help="Set log level to DEBUG"
)
argparser.add_argument(
    "--seed",
    type=int,
    default=123454321,
    help="A seed for the contained random processes",
)


def prepare(args):
    global gateway
    global data
    if args.debug:
        logging.basicConfig(level=logging.DEBUG)
        logging.getLogger()
        logging.getLogger("faker").setLevel(logging.WARNING)
        logging.getLogger("geventwebsocket.handler").setLevel(logging.WARNING)
    gateway = GatewayClient(
        args.quickHost, args.gateway, args.apiKey
    )
    data = ListenData(args.seed)
    # lambda is important here to get session-specific behaviour
    app.layout = lambda: main_layout(
        make_client_user(),
        make_user_radio(),
        make_demo_users(),
        make_user_details_pane(),
        make_functionals(),
    )


def guni_run(**kwargs):
    args_ = argparser.parse_args([f"--{a}={v}" for a, v in kwargs.items()])
    prepare(args_)
    if args_.port != 8050:
        warnings.warn(
            "When running through gunicorn, please specify port in the bind parameter of the gunicorn call"
        )
    return app.server


if __name__ == "__main__":
    cli_args = argparser.parse_args()
    prepare(cli_args)
    sys.stderr.write("Launching app with a single worker.\n")
    sys.stderr.write(
        "For multiple workers, run with gunicorn 'app.main:guni_run(**kwargs) -w [WORKERS]"
    )
    websockets.run_server(app, port=cli_args.port)
