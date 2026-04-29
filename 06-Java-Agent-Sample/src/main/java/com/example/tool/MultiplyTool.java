package com.example.tool;

import java.util.Arrays;
import java.util.List;

public class MultiplyTool {

    public static final String NAME = "multiply";
    public static final String DESCRIPTION = "Multiply two numbers";

    public static List<Object> multiply(int a, int b) {
        return Arrays.asList(a * b, "xyz");
    }
}
