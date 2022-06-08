import os
from pathlib import Path
from collections import defaultdict
from argparse import ArgumentParser
from itertools import chain
import logging

logger = logging.getLogger("LFM-Sampling")

# need to install in env
import pandas as pd


def get_listening_event_sample(
    path=Path("."),
    read_users=500,
    frac=1,
    min_avg_listeners=0,
    chunksize=10000,
    **kwargs,
):
    """
    Read the listening events file
    """
    users_tracks = set()
    logger.info(f"Reading {Path(path) / 'LFM-1b_LEs.txt'}...")
    les = pd.read_csv(
        Path(path) / "LFM-1b_LEs.txt",
        sep="\t",
        iterator=True,
        chunksize=chunksize,
        header=None,
        names=["user_id", "artist_id", "album_id", "track_id", "timestamp"],
    )
    ret = []
    readlines = 0
    for chunk in les:
        readlines += len(chunk)
        logger.debug(f"Parsed {readlines} lines")
        chunk = chunk.sample(frac=frac, random_state=179)
        users_tracks.update(zip(chunk.user_id, chunk.track_id))
        ret.append(chunk)
        nusers = len({u for u, _ in users_tracks})
        logger.debug(f"Collected data for {nusers} user")
        if nusers >= read_users:
            users_tracks = pd.DataFrame.from_records(iter(users_tracks))
            # how many users listened to a track at least once?
            track_listeners = users_tracks.groupby(1).size()
            # per user, how many other users listened to the same tracks, on average
            # should handle connectivity of the recommendation graph
            avg_other_listeners = (
                users_tracks.groupby(0)[1]
                .apply(
                    lambda tracks: pd.Series([track_listeners[t] - 1 for t in tracks])
                )
                .groupby(level=0)
                .mean()
                .sort_values(ascending=False)
            )
            logger.info(
                f"Total avg other listeners before filtering: {avg_other_listeners.mean():.2f}"
            )
            # cut of users that are too 'individual'
            avg_other_listeners = avg_other_listeners[
                avg_other_listeners >= min_avg_listeners
            ]

            logger.info(
                f"Total avg other listeners after filtering: {avg_other_listeners.mean():.2f}"
            )
            if avg_other_listeners.empty:
                raise RuntimeError(
                    "Could not find any users meeting the requirements. Maybe adapt parameters"
                )
            df = pd.concat([r[r.user_id.isin(avg_other_listeners.index)] for r in ret])
            logger.info(
                f"Sampled {len(df)} listening events for {len(df.user_id.unique())} users"
            )
            return df


def get_ids(path, le_sample: pd.DataFrame, field, chunksize=10000, **kwargs):
    ret = {}
    names = {
        "artist": ["artist_id", "artist"],
        "album": ["album_id", "album", "artist_id"],
        "track": ["track_id", "track", "artist_id"],
    }[field]
    subset = []
    required_ids = set(le_sample[field + "_id"])
    logger.info(
        f"Collecting {field}-ids from { Path(path) / ('LFM-1b_' + field + 's.txt')}"
    )
    for chunk in pd.read_csv(
        Path(path) / f"LFM-1b_{field}s.txt",
        sep="\t",
        iterator=True,
        chunksize=chunksize,
        header=None,
        names=names,
    ):
        subset.append(chunk[chunk[field + "_id"].isin(required_ids)])
    return pd.concat(subset)


if __name__ == "__main__":
    argparser = ArgumentParser()
    argparser.add_argument(
        "DATA",
        type=str,
        help="Path to the directory containing the original LFM-1b data",
    )
    argparser.add_argument(
        "-o",
        "--output",
        type=str,
        default="./LFM-1b-sample",
        help="Destination directory of the dataset sample",
    )
    argparser.add_argument(
        "--read-users",
        type=int,
        default=400,
        help="Read the head of the dataset, until (approximately) <read_users> have been scanned",
    )
    argparser.add_argument(
        "--chunksize",
        type=int,
        default=100000,
        help="When reading the csv files, read <chunksize> lines at once",
    )
    argparser.add_argument(
        "--frac",
        type=float,
        default=0.3,
        help="Only retain fraction of the original rows",
    )
    argparser.add_argument(
        "--min-avg-listeners",
        type=float,
        default=4,
        help="For connectivity in small samples, only retain a user if their tracks are listened on avg by this many other users",
    )
    argparser.add_argument(
        "-l", "--log-level", default=logging.INFO, type=int, help="Set log level"
    )
    args = argparser.parse_args()
    logging.basicConfig(level=args.log_level)
    if not os.path.isdir(args.output):
        os.makedirs(args.output)
    les = get_listening_event_sample(args.DATA, **vars(args))
    for name, data in chain(
        [
            ("LE", les),
        ],
        (
            (field, get_ids(args.DATA, les, field, **vars(args)))
            for field in ("artist", "album", "track")
        ),
    ):
        with open(Path(args.output) / f"LFM-1b_{name}s-sample.txt", "wb") as dest:
            data.to_csv(dest, header=False, index=False, sep="\t")
