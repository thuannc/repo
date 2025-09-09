package jsonpath;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonPathLib {
    // Static cache for parsed paths
    private static final Map<String, List<String>> PATH_CACHE = new HashMap<>();
    
    // ScriptEngine for evaluating filter expressions
    private static final ScriptEngine SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("JavaScript");

    /**
     * Find values in JSON data using JSONPath expression.
     * @param path JSONPath string (e.g., "$.store.books[?(@.price<10)].title")
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
            return new ArrayList<>(PATH_CACHE.get(path)); // Return copy to avoid mutation
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
                i++; // Skip '['
                StringBuilder bracketContent = new StringBuilder();
                boolean isFilter = false;
                if (i < path.length() && path.charAt(i) == '?') {
                    isFilter = true;
                    bracketContent.append(path.charAt(i)); // Include '?'
                    i++; // Skip '?'
                    while (i < path.length() && path.charAt(i) != ')') {
                        bracketContent.append(path.charAt(i));
                        i++;
                    }
                    if (i < path.length()) {
                        i++; // Skip ')'
                    }
                    bracketContent.append(')'); // Close filter
                } else {
                    while (i < path.length() && path.charAt(i) != ']') {
                        bracketContent.append(path.charAt(i));
                        i++;
                    }
                    i++; // Skip ']'
                }
                
                if (isFilter) {
                    segments.add("[?" + bracketContent.toString() + "]");
                } else if (bracketContent.toString().equals("*")) {
                    segments.add("[*]");
                } else {
                    segments.add("[" + bracketContent.toString() + "]");
                }
            } else {
                current.append(c);
                i++;
            }
        }
        if (current.length() > 0) {
            segments.add(current.toString());
        }

        PATH_CACHE.put(path, new ArrayList<>(segments)); // Cache copy
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
                // After .., we stop processing further segments here as recursive handles it
                break;
            } else if (segment.startsWith("[")) {
                for (Object result : results) {
                    if (result instanceof JSONArray) {
                        JSONArray array = (JSONArray) result;
                        if (segment.equals("[*]")) {
                            for (int j = 0; j < array.length(); j++) {
                                nextResults.add(array.get(j));
                            }
                        } else if (segment.startsWith("[?")) {
                            // Apply filter
                            String filterExpr = extractFilterExpression(segment);
                            List<Object> filtered = applyFilter(array, filterExpr);
                            nextResults.addAll(filtered);
                        } else {
                            // Handle index
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
                // Handle object key
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
     * Extract the filter expression from segment like "[?(expression)]".
     */
    private static String extractFilterExpression(String segment) {
        // Remove "[?" and "]"
        return segment.substring(2, segment.length() - 1);
    }

    /**
     * Apply filter to JSONArray using the expression.
     */
    private static List<Object> applyFilter(JSONArray array, String expression) {
        List<Object> filtered = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object item = array.get(i);
            if (item instanceof JSONObject) {
                try {
                    if (evaluateFilterExpression((JSONObject) item, expression)) {
                        filtered.add(item);
                    }
                } catch (ScriptException | IllegalArgumentException e) {
                    // Ignore invalid expressions for this item
                }
            }
        }
        return filtered;
    }

    /**
     * Evaluate the filter expression using JavaScript engine.
     * @param item The current JSONObject to evaluate against (@)
     * @param expression The filter expression (e.g., "@.price < 10")
     * @return true if the expression evaluates to true
     */
    private static boolean evaluateFilterExpression(JSONObject item, String expression) throws ScriptException {
        if (expression == null || expression.trim().isEmpty()) {
            return true;
        }

        // Replace @ with a JS representation of the item
        String jsExpression = expression.replace("@", "item");
        
        // Add contains support if needed (for strings/arrays)
        jsExpression = addContainsSupport(jsExpression, item);

        // Bind item to JS context
        SCRIPT_ENGINE.put("item", convertToJSObject(item));
        
        // Evaluate
        Object result = SCRIPT_ENGINE.eval(jsExpression);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Convert JSONObject to a JS-compatible object (recursive).
     */
    private static Object convertToJSObject(Object obj) {
        if (obj instanceof JSONObject) {
            Map<String, Object> map = new HashMap<>();
            JSONObject jsonObj = (JSONObject) obj;
            for (String key : jsonObj.keySet()) {
                map.put(key, convertToJSObject(jsonObj.get(key)));
            }
            return map;
        } else if (obj instanceof JSONArray) {
            List<Object> list = new ArrayList<>();
            JSONArray array = (JSONArray) obj;
            for (int i = 0; i < array.length(); i++) {
                list.add(convertToJSObject(array.get(i)));
            }
            return list;
        }
        return obj; // Primitive types
    }

    /**
     * Add support for 'contains' operator if used in expression.
     * For simplicity, replace 'item.property contains "value"' with appropriate JS.
     */
    private static String addContainsSupport(String expression, JSONObject item) {
        // Simple regex replacement for contains (extend as needed)
        // e.g., @.tags contains "fantasy" -> item.tags && item.tags.indexOf("fantasy") !== -1
        return expression.replaceAll("contains\\s+([^)]+)", "indexOf($1) !== -1");
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
    package jsonpath;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonPathExample {
    public static void main(String[] args) {
        // Sample JSON with books
        String jsonString = "{" +
                "\"store\": {" +
                "  \"books\": [" +
                "    {\"title\": \"Book 1\", \"author\": \"Author 1\", \"price\": 10, \"tags\": [\"fiction\", \"cheap\"]}," +
                "    {\"title\": \"Book 2\", \"author\": \"Author 2\", \"price\": 20, \"tags\": [\"non-fiction\"]}," +
                "    {\"title\": \"Book 3\", \"author\": \"Author 1\", \"price\": 5, \"tags\": [\"fantasy\"]}" +
                "  ]" +
                "}}";
        
        JSONObject json = new JSONObject(jsonString);

        // Test basic queries
        System.out.println("Query: $.store.books[0].title");
        System.out.println(JsonPathLib.find("$.store.books[0].title", json)); // [Book 1]

        // Test filter: books with price < 10
        System.out.println("Query: $.store.books[?(@.price<10)]");
        System.out.println(JsonPathLib.find("$.store.books[?(@.price<10)]", json)); // [Book 3 object]

        // Test filter with == and AND
        System.out.println("Query: $.store.books[?(@.author==\"Author 1\" && @.price>5)]");
        System.out.println(JsonPathLib.find("$.store.books[?(@.author==\"Author 1\" && @.price>5)]", json)); // [Book 1 object]

        // Test contains (for array)
        System.out.println("Query: $.store.books[?(@.tags contains \"fantasy\")]");
        System.out.println(JsonPathLib.find("$.store.books[?(@.tags contains \"fantasy\")]", json)); // [Book 3 object]

        // Recursive with filter
        System.out.println("Query: $..books[?(@.price<15)].title");
        System.out.println(JsonPathLib.find("$..books[?(@.price<15)].title", json)); // [Book 1, Book 3]
    }
}
}
