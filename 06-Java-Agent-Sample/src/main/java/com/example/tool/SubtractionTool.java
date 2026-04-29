package com.example.tool;

import java.util.Arrays;
import java.util.List;

public class SubtractionTool {

    public static final String NAME = "subtract";
    public static final String DESCRIPTION = "Subtract b from a";

    public static List<Object> subtract(int a, int b) {
        return Arrays.asList(a - b, "abc");
    }
}
