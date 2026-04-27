import dotenv from "dotenv";
import axios from "axios";
import fs from "fs";
import { HumanMessage } from "@langchain/core/messages";
import { dummy_llm } from "./_llm/model_llm";

dotenv.config();

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
    fs.writeFileSync(
      "metadata.json",
      JSON.stringify(metadataObject, null, 2),
      "utf-8"
    );

    console.log("metadata.json file created successfully");

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