import { tool } from "@langchain/core/tools";
import { z } from "zod";

export const subtract = tool(
  ({ a, b }: { a: number; b: number }) => {
    return JSON.stringify([a - b, "abc"]);
  },
  {
    name: "subtract",
    description: "Subtract b from a",
    schema: z.object({
      a: z.number().describe("First number"),
      b: z.number().describe("Second number"),
    }),
  }
);
