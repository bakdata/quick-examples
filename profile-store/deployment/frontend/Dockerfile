FROM python:3.9-slim-buster
ENV PYTHONPATH=$PYTHONPATH:$PWD
RUN apt-get update
# needed for pyavataaars
RUN apt-get install libcairo2-dev -y
COPY frontend/pyproject.toml frontend/poetry.lock /
RUN pip3 install poetry
RUN poetry config virtualenvs.create false
RUN poetry install --no-dev
COPY data/LFM-1b-sample /data
COPY frontend/app /
