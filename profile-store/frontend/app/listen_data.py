import importlib.resources
from io import StringIO
from collections import namedtuple

import pandas as pd

from app.generate_users import fake_users

ListeningEvent = namedtuple(
    "ListeningEvent", ["user_id", "artist_id", "album_id", "track_id", "timestamp"]
)


class ListenData:
    UNNAMED = "N.N."

    def __init__(self, seed: int = None):
        data = [
            StringIO(importlib.resources.read_text("data", f"LFM-1b_{d}-sample.txt"))
            for d in ["artists", "albums", "tracks", "LEs"]
        ]
        csv_kwargs = dict(sep="\t", header=None, keep_default_na=False, na_values="")
        self.artists = pd.read_csv(
            data[0],
            **csv_kwargs,
            names=["artist_id", "artist_name"],
        )
        self.artists.artist_name.fillna(ListenData.UNNAMED, inplace=True)

        self.albums = pd.read_csv(
            data[1],
            **csv_kwargs,
            names=["album_id", "album_name", "artist_id"],
        )
        self.albums.album_name.fillna(ListenData.UNNAMED, inplace=True)

        self.tracks = pd.read_csv(
            data[2],
            **csv_kwargs,
            names=["track_id", "track_name", "artist_id"],
        )
        self.tracks.track_name.fillna(ListenData.UNNAMED, inplace=True)

        listening_events = pd.read_csv(
            data[3],
            **csv_kwargs,
            names=["user_id", "artist_id", "album_id", "track_id", "timestamp"],
            usecols=["user_id", "artist_id", "album_id", "track_id"],
        )
        self.users = fake_users(listening_events.user_id.unique(), seed=seed)

        self.track_full_info = (
            listening_events[["artist_id", "album_id", "track_id"]]
            .drop_duplicates()
            .merge(
                self.artists[["artist_id", "artist_name"]],
                left_on="artist_id",
                right_on="artist_id",
            )
            .merge(
                self.albums[["album_id", "album_name"]],
                left_on="album_id",
                right_on="album_id",
            )
            .merge(
                self.tracks[["track_id", "track_name"]],
                left_on="track_id",
                right_on="track_id",
            )
            .sample(frac=1, random_state=seed)
        )

        self.artists.set_index("artist_id", inplace=True)
        self.albums.set_index("album_id", inplace=True)
        self.tracks.set_index("track_id", inplace=True)

    def get_track_fullinfo(self, i: int) -> pd.Series:
        return self.track_full_info.iloc[i]

    def get_id(self, name, kind):
        table = getattr(self, f"{kind}s")
        try:
            return table.loc[:, name, :].index[0][1]
        except (IndexError, KeyError):
            raise KeyError(name)

    def get_name(self, id_, kind):
        table = getattr(self, f"{kind}s")
        try:
            return table.loc[id_].index[0]
        except IndexError:
            raise KeyError(id_)

    def get_associated_artist(self, id_, kind):
        table = getattr(self, f"{kind}s")
        artist_id = table.loc[id_, "artist_id"]
        return self.artists.loc[artist_id].artist_name
