package org.neo4j.cypher_rs;

import au.com.bytecode.opencsv.CSVWriter;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Hunger @since 09.10.13
 */
public class CypherRsPostCsvTest extends RestTestBase {

    public static final String KEY = "foo";
    public static final String WRITE_QUERY = "create (n:Node {name:{name}}) return n";
    public static final String WRITE_QUERY_COLS = "create (n:Node {name:{name},age:{age},male:{male}}) return n";

    private WebResource cypherRsPath;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        cypherRsPath = rootResource.path("test").path(KEY);
    }

    @Test
    public void testQueryNonExistingEndpoint() throws Exception {
        ClientResponse response = post(1,"id", "123");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testPostSingleLineCsv() throws Exception {
        cypherRsPath.put(ClientResponse.class, WRITE_QUERY);
        ClientResponse response = post(1,"name", "foobar");
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals(true, result.contains("\"nodes_created\":1"));
        Map<String,Object> data = Utils.readJson(result);
        assertEquals(1,data.get("nodes_created"));
        assertEquals(1,data.get("labels_added"));
        assertEquals(1,data.get("properties_set"));
        assertEquals(1,data.get("rows"));
        assertEquals(0,data.get("relationships_created"));
    }
    @Test
    public void testPostMultiLineCsv() throws Exception {
        cypherRsPath.put(ClientResponse.class, WRITE_QUERY);
        ClientResponse response = post(1,"name", "foo","bar","foobar");
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals(true, result.contains("\"nodes_created\":3"));
        Map<String,Object> data = Utils.readJson(result);
        assertEquals(3,data.get("nodes_created"));
        assertEquals(3,data.get("labels_added"));
        assertEquals(3,data.get("properties_set"));
        assertEquals(3,data.get("rows"));
        assertEquals(0,data.get("relationships_created"));
    }

    private ClientResponse post(int width, String...data) throws IOException {
        StringWriter writer = new StringWriter(10000);
        CSVWriter csv = new CSVWriter(writer);
        for (int start = 0;start < data.length; start+=width) {
            csv.writeNext(Arrays.copyOfRange(data,start,start + width));
        }
        csv.flush();
        csv.close();
        System.out.println(writer.toString());
        return cypherRsPath.entity(writer.toString(), MediaType.TEXT_PLAIN_TYPE).post(ClientResponse.class);
    }

    @Test
    public void testMultipleColumns() throws Exception {
        cypherRsPath.put(ClientResponse.class, WRITE_QUERY_COLS);
        ClientResponse response = post(3,"name","age","male",
                                          "Andres","21","true");
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals(true, result.contains("\"nodes_created\":1"));
        Map<String,Object> data = Utils.readJson(result);
        assertEquals(1,data.get("nodes_created"));
        assertEquals(1,data.get("labels_added"));
        assertEquals(3,data.get("properties_set"));
        assertEquals(1,data.get("rows"));
        assertEquals(0,data.get("relationships_created"));
    }
}
