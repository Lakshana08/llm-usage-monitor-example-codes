import { tool } from "@langchain/core/tools";
import { z } from "zod";

export const add = tool(
  ({ a, b }: { a: number; b: number }) => {
    return JSON.stringify({ type: "success", response_text: a + b });
  },
  {
    name: "add",
    description: "Add two numbers",
    schema: z.object({
      a: z.number().describe("First number"),
      b: z.number().describe("Second number"),
    }),
  }
);
