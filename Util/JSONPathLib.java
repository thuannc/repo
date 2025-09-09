package jsonpath;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonPathLib {
    // Static cache for parsed paths
    private static final Map<String, List<String>> PATH_CACHE = new HashMap<>();

    /**
     * Find values in JSON data using JSONPath expression.
     * @param path JSONPath string (e.g., "$.store.books[0].title")
     * @param jsonData JSON data (JSONObject or JSONArray)
     * @return List of matching values
     */
    public static List<Object> find(String path, Object jsonData) {
        if (path == null || path.isEmpty()) {
            List<Object> result = new ArrayList<>();
            result.add(jsonData);
            return result;
        }

        List<String> segments = parsePath(path);
        return evaluatePath(segments, jsonData);
    }

    /**
     * Parse JSONPath into segments.
     */
    private static List<String> parsePath(String path) {
        if (PATH_CACHE.containsKey(path)) {
            return PATH_CACHE.get(path);
        }

        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int i = 0;

        while (i < path.length()) {
            char c = path.charAt(i);
            if (c == '$') {
                if (current.length() > 0) {
                    segments.add(current.toString());
                    current = new StringBuilder();
                }
                segments.add("$");
                i++;
            } else if (c == '.') {
                if (current.length() > 0) {
                    segments.add(current.toString());
                    current = new StringBuilder();
                }
                if (i + 1 < path.length() && path.charAt(i + 1) == '.') {
                    segments.add("..");
                    i += 2;
                } else {
                    i++;
                }
            } else if (c == '[') {
                if (current.length() > 0) {
                    segments.add(current.toString());
                    current = new StringBuilder();
                }
                StringBuilder bracketContent = new StringBuilder();
                i++;
                while (i < path.length() && path.charAt(i) != ']') {
                    bracketContent.append(path.charAt(i));
                    i++;
                }
                if (bracketContent.toString().equals("*")) {
                    segments.add("[*]");
                } else {
                    segments.add("[" + bracketContent.toString() + "]");
                }
                i++;
            } else {
                current.append(c);
                i++;
            }
        }
        if (current.length() > 0) {
            segments.add(current.toString());
        }

        PATH_CACHE.put(path, segments);
        return segments;
    }

    /**
     * Evaluate JSONPath segments against JSON data.
     */
    private static List<Object> evaluatePath(List<String> segments, Object data) {
        List<Object> results = new ArrayList<>();
        results.add(data);
        int currentSegmentIdx = 0;

        while (currentSegmentIdx < segments.size()) {
            String segment = segments.get(currentSegmentIdx);
            List<Object> nextResults = new ArrayList<>();

            if (segment.equals("$")) {
                nextResults.add(data);
            } else if (segment.equals("..")) {
                for (Object result : results) {
                    if (result instanceof JSONObject || result instanceof JSONArray) {
                        nextResults.addAll(recursiveDescent(result, segments.subList(currentSegmentIdx + 1, segments.size())));
                    }
                }
                break;
            } else if (segment.startsWith("[")) {
                for (Object result : results) {
                    if (result instanceof JSONArray) {
                        JSONArray array = (JSONArray) result;
                        if (segment.equals("[*]")) {
                            for (int i = 0; i < array.length(); i++) {
                                nextResults.add(array.get(i));
                            }
                        } else {
                            String indexStr = segment.substring(1, segment.length() - 1);
                            try {
                                int index = Integer.parseInt(indexStr);
                                if (index >= 0 && index < array.length()) {
                                    nextResults.add(array.get(index));
                                }
                            } catch (NumberFormatException e) {
                                // Ignore invalid index
                            }
                        }
                    }
                }
            } else {
                for (Object result : results) {
                    if (result instanceof JSONObject) {
                        JSONObject obj = (JSONObject) result;
                        if (obj.has(segment)) {
                            nextResults.add(obj.get(segment));
                        }
                    }
                }
            }

            results = nextResults;
            currentSegmentIdx++;
        }

        return results;
    }

    /**
     * Perform recursive descent for '..' operator.
     */
    private static List<Object> recursiveDescent(Object data, List<String> remainingSegments) {
        List<Object> results = new ArrayList<>();
        if (remainingSegments.isEmpty()) {
            results.add(data);
            return results;
        }

        String nextSegment = remainingSegments.get(0);
        if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject) data;
            for (String key : obj.keySet()) {
                Object value = obj.get(key);
                if (!nextSegment.equals("..") && key.equals(nextSegment)) {
                    results.addAll(evaluatePath(remainingSegments.subList(1, remainingSegments.size()), value));
                }
                results.addAll(recursiveDescent(value, remainingSegments));
            }
        } else if (data instanceof JSONArray) {
            JSONArray array = (JSONArray) data;
            for (int i = 0; i < array.length(); i++) {
                results.addAll(recursiveDescent(array.get(i), remainingSegments));
            }
        }
        return results;
    }
  public static void main(String[] args) {
        // Sample JSON
        String jsonString = "{" +
                "\"store\": {" +
                "  \"books\": [" +
                "    {\"title\": \"Book 1\", \"author\": \"Author 1\", \"price\": 10}," +
                "    {\"title\": \"Book 2\", \"author\": \"Author 2\", \"price\": 20}" +
                "  ]" +
                "}}";
        
        JSONObject json = new JSONObject(jsonString);

        // Test JSONPath queries
        System.out.println("Query: $.store.books[0].title");
        System.out.println(JsonPathLib.find("$.store.books[0].title", json)); // Output: [Book 1]

        System.out.println("Query: $.store.books[*].author");
        System.out.println(JsonPathLib.find("$.store.books[*].author", json)); // Output: [Author 1, Author 2]

        System.out.println("Query: $..author");
        System.out.println(JsonPathLib.find("$..author", json)); // Output: [Author 1, Author 2]
    }
}
