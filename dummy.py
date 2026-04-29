import json

data = {
    "name": "Lakshana",
    "age": 22,
    "is_student": True
}

# Convert Python dict → JSON string
json_string = json.dumps(data)

print(type(json_string))  # Output: <class 'str'>
print(json_string)