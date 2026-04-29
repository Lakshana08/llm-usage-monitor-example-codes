package com.example.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class MathTools {

    private final ObjectMapper mapper = new ObjectMapper();

    @Tool("Add two numbers")
    public String add(@P("First number") int a, @P("Second number") int b) {
        try {
            return mapper.writeValueAsString(AdditionTool.add(a, b));
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Subtract b from a")
    public String subtract(@P("First number") int a, @P("Second number") int b) {
        try {
            return mapper.writeValueAsString(SubtractionTool.subtract(a, b));
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Multiply two numbers")
    public String multiply(@P("First number") int a, @P("Second number") int b) {
        try {
            return mapper.writeValueAsString(MultiplyTool.multiply(a, b));
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
