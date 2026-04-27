import dotenv from "dotenv";
import path from "path";
import axios from "axios";
import { HumanMessage } from "@langchain/core/messages";
import { saveMetadataToFile } from "./_util/file_ops";

dotenv.config({ path: path.resolve(__dirname, "../.env") });

function getLLM(temperature = 0) {
  return {
    async invoke(messages: any[]) {
      const tokenRes = await axios.post(
        process.env.AICORE_AUTH_URL!,
        "grant_type=client_credentials",
        {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
          auth: {
            username: process.env.AICORE_CLIENT_ID!,
            password: process.env.AICORE_CLIENT_SECRET!,
          },
        }
      );

      const token = tokenRes.data.access_token;

      const response = await axios.post(
        `${process.env.AICORE_BASE_URL}/inference/deployments/${process.env.LLM_DEPLOYMENT_ID}/chat/completions?api-version=2023-05-15`,
        {
          messages: messages.map((m: any) => ({
            role: "user",
            content: m.content,
          })),
          temperature,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
            "AI-Resource-Group": process.env.AICORE_RESOURCE_GROUP!,
          },
        }
      );

      return response.data;
    },
  };
}

const dummy_llm = getLLM(0.2);

/**
 * API URL
 */
const url =
  "https://llm-usage-monitor-egregious-armadillo-bt.cfapps.us10-001.hana.ondemand.com/log-metadata/?app_id=4&call_type=l_invoke";

/**
 * Prompt object
 */
const prompt = {
  instructions: "capital of america",
};

async function main() {
  try {
    /**
     * Call LLM
     */
    const response = await dummy_llm.invoke([
      new HumanMessage(JSON.stringify(prompt, null, 2)),
    ]);

    console.log("Type of response:", typeof response);
    console.log(JSON.stringify(response, null, 2));

    /**
     * Convert response object to JSON object
     */
    const metadataObject = response;

    /**
     * Save metadata to JSON file
     */
    saveMetadataToFile(metadataObject, "_temp/llm_response.json");

    /**
     * Convert to string for API payload
     */
    const metadata = JSON.stringify(metadataObject);

    const payload = {
      metadata: metadata,
    };

    const headers = {
      Authorization:
        "Bearer 59a7d10e140fabe8ee26f96ac5043f19a66e4e30f1895384a6029bdc5347e0dc",
      "Content-Type": "application/json",
    };

    /**
     * Send POST request
     */
    const apiResponse = await axios.post(url, payload, { headers });

    console.log("Status Code:", apiResponse.status);
    console.log("Response Data:", apiResponse.data);
  } catch (error: any) {
    console.error("Error:", error.response?.data || error.message);
  }
}

main();
