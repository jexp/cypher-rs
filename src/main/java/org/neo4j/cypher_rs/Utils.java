package org.neo4j.cypher_rs;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.server.rest.repr.BadInputException;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Michael Hunger @since 09.10.13
 */
public class Utils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final Pattern IS_WRITE_QUERY = Pattern.compile("(create|set|remove|merge|delete|drop)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    static Map<String, Object> toParams(MultivaluedMap<String, String> queryParameters) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            List<String> value = entry.getValue();
            if (value.isEmpty()) result.put(entry.getKey(), value);
            if (value.size() == 1)
                result.put(entry.getKey(), convertIfNeeded(value.get(0)));
            else {
                result.put(entry.getKey(), convertIfNeeded(value));
            }
        }
        return result;
    }

    private static List<Object> convertIfNeeded(List<String> value) {
        List<Object> result = new ArrayList<>(value.size());
        for (String element : value) {
            result.add(convertIfNeeded(element));
        }
        return result;
    }

    public static Object convertIfNeeded(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        if (value.matches("^[+-]?[0-9.]+$")) {
            if (value.contains(".")) return Double.parseDouble(value);
            return Long.parseLong(value);
        }
        return value;
    }

    static boolean isWriteQuery(String query) {
        return IS_WRITE_QUERY.matcher(query).find();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> toParams(String body) throws BadInputException {
        try {
            Object data = OBJECT_MAPPER.readValue(body, Object.class);
            if (data instanceof Map) return Arrays.asList((Map<String, Object>)data);
            if (data instanceof List) return (List<Map<String, Object>>)data;
            throw new BadInputException("Cannot read as JSON list or map: "+"\n"+body);
        } catch (IOException ioe) {
            throw new BadInputException("Cannot read as JSON list or map: "+ioe.getMessage()+"\n"+body);
        }
    }

    static String toJson(ExecutionResult result) throws IOException {
        return toJson(toObject(result));
    }

    public static Object toObject(ExecutionResult result) {
        return new CypherResultRenderer().render(result);
    }

    public static String toJson(Object value) throws IOException {
        if(value == null)
          return null;

        return OBJECT_MAPPER.writeValueAsString(value);
    }

    static void writeToJson(ExecutionResult result, OutputStream out) throws IOException {
        Object data = toObject(result);
        OBJECT_MAPPER.writeValue(out, data);
    }

    public static Map<String, Object> readJson(String json) throws IOException {
        return OBJECT_MAPPER.readValue(json,Map.class);
    }
}
