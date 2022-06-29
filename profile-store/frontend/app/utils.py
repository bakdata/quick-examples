import datetime
import random
from typing import List, Union

import pytz
from dash import html
from dash.development.base_component import Component
from dash_extensions import Ticker


class Colors:
    base = "transparent"
    button = "#9BFFD8"
    user_selected = "#ddfdf1"


button_style = dict(
    borderRadius="8px",
    backgroundColor=Colors.button,
    width="100%",
    height="50px",
    minWidth="50px",
    margin="5px",
    border="0px",
)


def timestamp2datetime(timestamp: int):
    return datetime.datetime.fromtimestamp(timestamp // 1000).astimezone(
        pytz.timezone("CET")
    )


def timestamp2last_active_status(dt: datetime.datetime):
    now = datetime.datetime.now(pytz.timezone("CET"))
    if dt.day == now.day:
        return dt.strftime("%H:%M")
    diff = now - dt
    if diff.days <= 1:
        return "yesterday"
    return dt.strftime("%m-%d-%y")


def format_song(
    track: str, artist: str = None, album: str = None
) -> Union[Component, List[Component]]:
    ret = [html.Span(html.I(track))]
    if artist:
        ret.extend([" by ", html.B(artist)])
    if album:
        ret.append(html.Span([" [", html.I(album), "]"]))
    return ret


def new_ticker(msg):
    return Ticker(
        html.Div(msg, style=dict(whiteSpace="nowrap")),
        mode="await",
        offset="run-in",
        direction="toLeft",
        speed=5,
        # fresh id is necessary to immediately display the new track in the ticker
        id=str(random.randint(0, 10 ** 9)),
    )
