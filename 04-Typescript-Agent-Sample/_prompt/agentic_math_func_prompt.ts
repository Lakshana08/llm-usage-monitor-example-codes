export const HUMAN_AGENTIC_PROMPT = (a: number, b: number): string => `
You have access to math tools.
Call only the appropriate tools needed as much it is needed, don't unwantedly call unwanted tools
just let the tools do the work, your job is just to select the tools and not to answer on your own, If no tools are needed, dont use any and also dont answer on your own for the prompt

Task:
1. Multiply the output of sum of ${a} and ${b} with the greatest number among inputs of ${a} and ${b}, and use that final answer
to subract it with the lowest number among inputs of ${a} and ${b}

Use tools when needed.
Return only the final number.
`;
