FROM python:3.9-slim-buster
ENV PYTHONPATH=$PYTHONPATH:$PWD
RUN apt-get update
# needed for pyavataaars
RUN apt-get install libcairo2-dev -y
RUN mkdir profile-store-app
WORKDIR /profile-store-app
COPY pyproject.toml poetry.lock /profile-store-app/
RUN pip3 install poetry
RUN poetry config virtualenvs.create false
RUN poetry install --no-dev
COPY __init__.py /profile-store-app/
COPY data /profile-store-app/data
COPY app /profile-store-app/app
