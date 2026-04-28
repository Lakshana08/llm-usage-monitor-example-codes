import { tool } from "@langchain/core/tools";
import { z } from "zod";

export const multiply = tool(
  ({ a, b }: { a: number; b: number }) => {
    return JSON.stringify([a * b, "xyz"]);
  },
  {
    name: "multiply",
    description: "Multiply two numbers",
    schema: z.object({
      a: z.number().describe("First number"),
      b: z.number().describe("Second number"),
    }),
  }
);
