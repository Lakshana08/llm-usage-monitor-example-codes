import dotenv from "dotenv";
import path from "path";
import axios from "axios";
import { HumanMessage } from "@langchain/core/messages";
import { ChatOpenAI } from "@langchain/openai";
import { createReactAgent } from "@langchain/langgraph/prebuilt";
import { add } from "./_tool/addition_tool";
import { subtract } from "./_tool/subtraction_tool";
import { multiply } from "./_tool/multiply_tool";
import { HUMAN_AGENTIC_PROMPT } from "./_prompt/agentic_math_func_prompt";
import { saveMetadataToFile } from "./_util/file_ops";

dotenv.config({ path: path.resolve(__dirname, "../.env") });

async function getAccessToken(): Promise<string> {
  const tokenRes = await axios.post(
    process.env.AICORE_AUTH_URL!,
    "grant_type=client_credentials",
    {
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      auth: {
        username: process.env.AICORE_CLIENT_ID!,
        password: process.env.AICORE_CLIENT_SECRET!,
      },
    }
  );
  return tokenRes.data.access_token;
}

const prompt = {
  instructions: HUMAN_AGENTIC_PROMPT(1, 2),
};

async function main() {
  try {
    const token = await getAccessToken();

    const llm = new ChatOpenAI({
      modelName: "gpt-4o",
      openAIApiKey: token,
      configuration: {
        baseURL: `${process.env.AICORE_BASE_URL}/inference/deployments/${process.env.LLM_DEPLOYMENT_ID}`,
        defaultHeaders: {
          "AI-Resource-Group": process.env.AICORE_RESOURCE_GROUP!,
        },
        defaultQuery: { "api-version": "2023-05-15" },
      },
    });

    const agent = createReactAgent({
      llm,
      tools: [add, subtract, multiply],
    });

    const response = await agent.invoke({
      messages: [new HumanMessage(JSON.stringify(prompt, null, 2))],
    });

    console.log(typeof response);
    console.log(response);

    saveMetadataToFile(response, "_temp/agentic_math_func_response.json");

    const payload = {
      metadata: JSON.stringify(response),
    };

    const headers = {
      Authorization:
        "Bearer 59a7d10e140fabe8ee26f96ac5043f19a66e4e30f1895384a6029bdc5347e0dc",
      "Content-Type": "application/json",
    };

    const apiResponse = await axios.post(
      `${process.env.URL}/log-metadata/?app_id=4&call_type=a_invoke`,
      payload,
      { headers }
    );

    console.log("Status Code:", apiResponse.status);
    console.log("Response Data:", apiResponse.data);
  } catch (error: any) {
    console.error("Error:", error.response?.data || error.message);
  }
}

main();
