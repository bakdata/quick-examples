import base64
import random

import pandas as pd
import py_avataaars as pa
from faker import Faker


def create_avatar(g="any") -> str:
    """
    Create an py_avataaars avatar. Not totally random, to look more conventional.

    :param g: gender ["m", "f", "any"]
    :return: the base64 encoded avatar (png)
    """
    # beards only for men
    facial_hair = list(pa.FacialHairType) if g == "m" else [pa.FacialHairType.DEFAULT]
    # not too eccentric expressions
    eyes = [pa.EyesType.HAPPY, pa.EyesType.DEFAULT, pa.EyesType.SURPRISED]
    mouth = [pa.MouthType.DEFAULT, pa.MouthType.SMILE, pa.MouthType.TWINKLE]
    top_type = list(pa.TopType)
    # no eyepatches
    top_type.remove(pa.TopType.EYE_PATCH)
    # no hijab for men
    if g == "m":
        top_type.remove(pa.TopType.HIJAB)
    # no turban for women
    if g == "f":
        top_type.remove(pa.TopType.TURBAN)

    def choose(choices):
        return random.choice(list(choices))

    # not so much pink or platinum hair
    special = [pa.HairColor.PASTEL_PINK, pa.HairColor.PLATINUM]
    hair_color = (
        choose(special)
        if random.random() > 0.95
        else choose([p for p in pa.HairColor if p not in special])
    )
    top = choose(top_type)

    # decrease prob of long-haired men a bit
    if g == "m" and "LONG" in str(top):
        top = choose(top_type)

    avatar = pa.PyAvataaar(
        style=pa.AvatarStyle.TRANSPARENT,
        skin_color=choose(pa.SkinColor),
        hair_color=hair_color,
        facial_hair_type=choose(facial_hair),
        facial_hair_color=hair_color,
        top_type=top,
        hat_color=choose(pa.Color),
        mouth_type=choose(mouth),
        eye_type=choose(eyes),
        eyebrow_type=choose(pa.EyebrowType),
        nose_type=pa.NoseType.DEFAULT,
        accessories_type=pa.AccessoriesType.DEFAULT
        if random.random() < 0.5
        else choose(pa.AccessoriesType),
        clothe_type=choose(pa.ClotheType),
        clothe_color=choose(pa.Color),
        clothe_graphic_type=choose(pa.ClotheGraphicType),
    ).render_png()
    return base64.b64encode(avatar).decode()


def fake_users(user_ids, seed=None):
    faker_ = Faker()
    Faker.seed(seed)
    random.seed(seed)
    names = []
    avatars = []
    binary = int(len(user_ids) * 0.45)
    for i in range(binary):
        names.extend([faker_.name_male(), faker_.name_female()])
        avatars.extend([create_avatar(g="m"), create_avatar(g="f")])
    for i in range(2 * binary, len(user_ids)):
        names.append(faker_.name_nonbinary())
        avatars.append(create_avatar(g="any"))
    return pd.DataFrame(
        {"avatars": avatars}, index=pd.MultiIndex.from_tuples(zip(user_ids, names))
    )
