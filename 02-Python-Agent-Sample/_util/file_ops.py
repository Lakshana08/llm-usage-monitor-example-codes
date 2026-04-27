import os
import json
import tempfile
from typing import Tuple

def write_json(json_value, json_file_name):
    try:
        base_dir = os.getcwd()
        temp_dir = os.path.join(base_dir, "02-Python-Agent-Sample/_temp")
        os.makedirs(temp_dir, exist_ok=True)
        json_path = os.path.join(temp_dir, json_file_name)
        
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(json_value, f, indent=4)

    except Exception as e:
        print(f"Exception in write_json function: {e}")

    finally:
        pass

