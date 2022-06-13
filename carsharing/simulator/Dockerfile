FROM python:3.8 as runner

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt


RUN mkdir data car_sharing_simulator

COPY ./data/status.jsonl ./data/
COPY ./car_sharing_simulator/ ./car_sharing_simulator/


ENTRYPOINT ["python", "-m", "car_sharing_simulator.simulator"]