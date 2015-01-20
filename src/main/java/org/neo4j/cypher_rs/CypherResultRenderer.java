package org.neo4j.cypher_rs;

import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.ResourceIterator;

import java.util.*;

/**
 * @author Michael Hunger @since 10.10.13
 */
@SuppressWarnings("unchecked")
public class CypherResultRenderer {

    public Object render(ExecutionResult result) {
        try (ResourceIterator<Map<String, Object>> it = result.iterator()) {

            Object object = null;

            if(it.hasNext()) {
                object = convertRows(it);
            }

            return object;


        }
    }

    Object convertRows(Iterator<Map<String, Object>> rows) {
        List<Object> list = new ArrayList<>();
        while (rows.hasNext()) {
            list.add(convert(rows.next()));
        }
        return list;
    }

    Map<String,Object> convert(Map<String,Object> map) {
        Map<String,Object> result=new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), convert(entry.getValue()));
        }
        return result;
    }

    Object convert(Object value) {
        if (value == null) return null;
        if (value instanceof Map) {
            return convert((Map<String, Object>) value);
        }
        if (value instanceof Path) {
            return convert(((Path) value).iterator());
        }
        if (value instanceof Iterator) {
            return convert((Iterator) value);
        }
        if (value instanceof List) {
            return convert((List<Object>) value);
        }
        if (value instanceof Iterable) {
            return convert(((Iterable) value).iterator());
        }
        if (value instanceof PropertyContainer) {
            return convert((PropertyContainer) value);
        }
        return value;
    }

    Object convert(List<Object> list) {
        ArrayList<Object> result = new ArrayList<>(list.size());
        for (Object element : list) {
            result.add(convert(element));
        }
        return result;
    }

    Object convert(Iterator it) {
        List<Object> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(convert(it.next()));
        }
        return result;
    }

    Map<String, Object> convert(PropertyContainer pc) {
        Iterator<String> keys = pc.getPropertyKeys().iterator();
        if (!keys.hasNext()) return Collections.EMPTY_MAP;

        Map<String, Object> result = new TreeMap<>();
        while (keys.hasNext()) {
            String prop = keys.next();
            result.put(prop, pc.getProperty(prop));
        }
        return result;
    }
}
