import dotenv from "dotenv";
import axios from "axios";

dotenv.config();

export function getLLM(temperature = 0) {
  return {
    async invoke(messages: any[]) {
      // Get OAuth token
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

      // Call SAP AI Core deployment
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