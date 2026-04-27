import json, os
import requests
from dotenv import load_dotenv
from _util.file_ops import write_json
from _tool.addition_tool import add
from _tool.subtraction_tool import subtract
from _tool.multiply_tool import multiply
from _prompt.agentic_math_func_prompt import HUMAN_AGENTIC_PROMPT
from langchain_core.messages import HumanMessage
from langchain_core.load import dumps as lc_dumps
from langgraph.prebuilt import create_react_agent
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
            "instructions": HUMAN_AGENTIC_PROMPT.format(1, 2)
        }
llm = ChatOpenAI(
        deployment_id=LLM_DEPLOYMENT_ID
    )
agent = create_react_agent(
                    model=llm,
                    tools=[add, subtract, multiply],
                )
response = agent.invoke({
    "messages": [HumanMessage(content=json.dumps(prompt, indent=2))]
})

write_json(json_value = str(response), json_file_name = "agentic_math_func_response.json")
print(type(response))
print(response)

#################################
lc_response = lc_dumps(response)
print(type(lc_response))
print(lc_response)

#################################

payload = {
            "metadata": lc_dumps(response)
        }

headers = {
    "Authorization": "Bearer 59a7d10e140fabe8ee26f96ac5043f19a66e4e30f1895384a6029bdc5347e0dc",
    "Content-Type": "application/json"
}

response = requests.post(f"{URL}/log-metadata/?app_id=4&call_type=a_invoke", json=payload, headers=headers)

print(response.status_code)
print(response.json())