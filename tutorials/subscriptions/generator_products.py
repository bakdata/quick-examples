from mimesis.schema import Field, Schema
import json
import random

ids = []
_ = Field('en')


def create_product():
    product_id = _('numbers.integer_number', start=1)
    ids.append(product_id)

    return {
        'key': product_id,
        'value': {
            'productId': product_id,
            'name': _('food.fruit'),
            'description': _('text.sentence'),
            'price': {
                'total': _('numbers.float_number', precision=2),
                'currency': _('choice', items=['DOLLAR', 'EURO', 'CHE'])
            },
            'metadata': {
                'created_at': _('datetime.timestamp'),
                'source': _('internet.home_page')
            }
        }
    }


product_schema = Schema(schema=create_product)
product_data = product_schema.create(iterations=15)

with open('ids.json', 'w') as id_file:
    json.dump({'ids': ids}, id_file, indent=4)

with open('products.json', 'w') as product_file:
    json.dump(product_data, product_file, indent=4)
