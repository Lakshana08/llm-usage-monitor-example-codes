import json, os
import requests
from dotenv import load_dotenv
from _util.file_ops import write_json
from langchain_core.messages import HumanMessage
from gen_ai_hub.proxy.langchain.openai import ChatOpenAI

load_dotenv()

AICORE_AUTH_URL = os.getenv("AICORE_AUTH_URL")
AICORE_CLIENT_ID = os.getenv("AICORE_CLIENT_ID")
AICORE_CLIENT_SECRET = os.getenv("AICORE_CLIENT_SECRET")
AICORE_RESOURCE_GROUP = os.getenv("AICORE_RESOURCE_GROUP")
AICORE_BASE_URL = os.getenv("AICORE_BASE_URL")
LLM_DEPLOYMENT_ID = os.getenv("LLM_DEPLOYMENT_ID")
URL = os.getenv("URL")

prompt = {
    "instructions": "capital of america"
}
llm = ChatOpenAI(
        deployment_id=LLM_DEPLOYMENT_ID
    )
response = llm.invoke([
    HumanMessage(content=json.dumps(prompt, indent=2))
])

write_json(json_value = dict(response), json_file_name = "llm_response.json")

print(type(response))
print(response)

# Convert response to string
metadata = json.dumps(response.model_dump(), default=str)
print(type(metadata))
print(metadata)

#################################

payload = {
    "metadata": metadata
}

headers = {
    "Authorization": "Bearer 59a7d10e140fabe8ee26f96ac5043f19a66e4e30f1895384a6029bdc5347e0dc",
    "Content-Type": "application/json"
}

response = requests.post(f"{URL}/log-metadata/?app_id=4&call_type=l_invoke", json=payload, headers=headers)

print(response.status_code)
print(response.json())