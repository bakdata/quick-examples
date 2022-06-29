from dash import html

from utils import button_style, new_ticker

rowL = dict(display="flex", flexFlow="row nowrap")


def make_user(
    user_id: int,
    name,
    avatar: str,
    order: int,
    updatable: bool = False,
    user_type: str = "demo",
):
    """
    :param user_id:
    :param name:
    :param avatar: the base64-encoded avatar
    :param order: the position in the user grid
    :param updatable: if True, add Button for re-generating user avatar
    :param user_type: ['demo', 'custom'] user types (for cb control)
    """
    user_id = {"id": int(user_id), "user_type": user_type}

    name_comp = html.Div(
        name,
        id={"type": "user_name", **user_id},
        style=dict(fontSize="x-large", margin="5px"),
    )
    update_button = (
        html.Button(
            "\u21bb", id={"type": "avatar_refresh", **user_id}, style=button_style
        )
        if updatable
        else None
    )
    avatar_comp = html.Img(
        src=f"data:image/png;base64,{avatar}", id={"type": "user_avatar", **user_id}
    )
    charts_comp = html.Button(
        "Charts", id={"type": "charts_button", **user_id}, style=button_style
    )
    recomm_comp = html.Button(
        "Recommended",
        id={"type": "recommendation_button", **user_id},
        style=button_style,
    )
    info_comp = html.Div(
        [
            html.P(
                [
                    "Last active: ",
                    html.Strong(
                        "-",
                        id={"type": "last_listen", **user_id},
                    ),
                ],
                style=dict(fontSize="95%"),
            )
        ],
        style=dict(
            display="flex",
            flexDirection="column",
            justifyContent="flex-start",
            width="20%",
        ),
        id={"type": "info", **user_id},
    )
    user_track_comp = html.Div(
        # when we set a new track, we set an entire new ticker, otherwise it
        # does not look like immediately switching
        new_ticker("     +++ not listening +++     "),
        id={"type": "current_listen", **user_id},
    )
    return make_user_card(
        user_id,
        name_comp,
        charts_comp,
        recomm_comp,
        avatar_comp,
        info_comp,
        user_track_comp,
        order,
        update_button,
    )


def make_user_card(
    user_id,
    name,
    charts,
    recomm,
    img,
    info,
    user_track,
    order,
    update_button=None,
):
    """
    Arrange the functional components to a single component per user
    """
    if update_button:
        update_button.style = {
            **button_style,
            **dict(position="absolute", top=0, left=0, width="auto"),
        }
        img = html.Div([img, update_button], style=dict(position="relative"))

    return html.Div(
        [
            html.Div([img, info], style=dict(width="100%", **rowL)),
            html.Div(name, style=dict(textAlign="center")),
            html.Div(
                user_track,
                style=dict(
                    width="330px", alignSelf="center", position="relative", left="15px"
                ),
            ),
            html.Div([charts, recomm], style=rowL),
        ],
        id={"type": "user_card", **user_id},
        style=dict(
            margin="15px",
            padding="10px",
            width="380px",
            borderRadius="8px",
            order=order,
        ),
    )
