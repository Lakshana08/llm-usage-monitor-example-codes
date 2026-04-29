package com.example.tool;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdditionTool {

    public static Map<String, Object> add(int a, int b) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "success");
        result.put("response_text", a + b);
        return result;
    }
}
