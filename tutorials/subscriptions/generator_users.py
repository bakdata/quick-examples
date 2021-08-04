from mimesis.schema import Field, Schema
from mimesis import Address, Person
import json
import random
import string

uids = []
_ = Field('en')

def id_generator(size=6, chars=string.ascii_lowercase + string.digits):
    id = ''.join(random.choice(chars) for _ in range(size))
    return id

def create_user():
    user_id = id_generator()
    uids.append(user_id)
    return {
        'key': user_id,
        'value': {
            'userId': user_id,
            'name': _('full_name'),
            'address': _('address'),
        }   
    }

user_schema = Schema(schema=create_user)
user_data = user_schema.create(iterations=15)

with open('user_ids.json', 'w') as uid_file:
    json.dump({'uids': uids}, uid_file, indent=4)

with open('users.json', 'w') as user_file:
    json.dump(user_data, user_file, indent=4)

print(json.dumps(user_data, indent=4))

