import requests
import time
from mimesis.schema import Field, Schema
import json
import random
import os

_ = Field('en')

with open(r"./ids.json") as id_file:
    ids = json.load(id_file)['ids']

with open(r"./user_ids.json") as id_file:
    user_ids = json.load(id_file)['uids']


def create_purchase():
    purchase_id = _('uuid')
    # take random product
    product_id = random.choice(ids)
    user_id = random.choice(user_ids)
    return {
        'key': purchase_id,
        'value': {
            'purchaseId': purchase_id,
            'productId': int(product_id),
            'userId': user_id,
            'amount': _('numbers.integer_number', start=1, end=20),
            'price': {
                'total': _('numbers.float_number', precision=2),
                'currency': _('choice', items=["DOLLAR", "EURO", "CHE"])
            }
        }
    }


purchase_schema = Schema(schema=create_purchase)
headers = {
    "Content-Type": "application/json",
    "X-API-Key": os.getenv("KEY"),
}
while True:
    purchase_data = purchase_schema.create()
    response = requests.post(f'http://{os.getenv("QUICK_HOST")}/ingest/purchases-topic',
                             json=purchase_data,
                             headers=headers)
    print(response.text)
    print('Added new purchase')
    time.sleep(20)

# with open('purchases.json', 'w') as purchase_file:
#     json.dumps(purchase_data, purchase_file, indent=4)
