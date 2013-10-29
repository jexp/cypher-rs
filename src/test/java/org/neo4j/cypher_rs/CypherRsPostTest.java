package org.neo4j.cypher_rs;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static java.util.Arrays.asList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author Michael Hunger @since 09.10.13
 */
public class CypherRsPostTest extends RestTestBase {

    public static final String KEY = "foo";
    public static final String MULTI_COLUMN_QUERY = "match n where id(n) in {ids} return length(n.name) as l, n.name as name";
    public static final String QUERY = "start n=node({id}) return n";
    public static final String WRITE_QUERY = "create (n:Node {name:{name}}) return n";
    
    //doing a node lookup by ID is a Cypher error, so it's a bit roundabout to represent more of a normal index lookup which wouldn't be a cypher error
    public static final String UPDATE_QUERY = "match n where id(n) = {id} SET n.name={name} return n";
    
    private WebResource cypherRsPath;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        cypherRsPath = rootResource.path("test").path(KEY);
    }

    private Node createNode(String name, String value) {
        try (Transaction tx = beginTx()) {
            Node node = getGraphDatabase().createNode();
            node.setProperty(name, value);
            tx.success();
            return node;
        }
    }

    @Test
    public void testQueryNonExistingEndpoint() throws Exception {
        ClientResponse response = post(map("id", 123));
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testQueryEndpoint() throws Exception {
        Node node=createNode("foo","bar");
        cypherRsPath.put(ClientResponse.class, QUERY);
        Map<String,Object> payload = map("id", node.getId());
        ClientResponse response = post(payload);
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals("{\"foo\":\"bar\"}", result);
    }

    @Test
    public void testQueryEndpointNoResults() throws Exception {
        
        cypherRsPath.put(ClientResponse.class, UPDATE_QUERY);
        Map<String,Object> payload = map("id", -234);
        payload.put("name", "Neo");
        
        ClientResponse response = post(payload);
        assertEquals(204, response.getStatus());
    }
    
    @Test
    public void testQueryEndpointMultiAddNoResults() throws Exception {
        
        cypherRsPath.put(ClientResponse.class, UPDATE_QUERY);
        Map<String,Object> payload1 = map("id", -234);
        payload1.put("name", "Neo");
        
        Map<String,Object> payload2 = map("id", -234);
        payload2.put("name", "Neo");
        
        List<Map<String,Object>> items = new ArrayList<>();
        items.add(payload1);
        items.add(payload2);
        
        ClientResponse response =  cypherRsPath.entity(Utils.toJson(items), MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class);
        String result = response.getEntity(String.class);
        assertEquals(200, response.getStatus());
        assertEquals("[null,null]", result);
    }
    
    @Test
    public void testQueryEndpointMultiAddSomeResults() throws Exception {
        
        Node node=createNode("name","bar");
        
        cypherRsPath.put(ClientResponse.class, UPDATE_QUERY);
        Map<String,Object> payload1 = map("id", node.getId());
        payload1.put("name", "Neo");
        
        Map<String,Object> payload2 = map("id", -234);
        payload2.put("name", "Neo2");
        
        List<Map<String,Object>> items = new ArrayList<>();
        items.add(payload1);
        items.add(payload2);
        
        ClientResponse response =  cypherRsPath.entity(Utils.toJson(items), MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class);
        String result = response.getEntity(String.class);
        assertEquals(200, response.getStatus());
        assertEquals("[{\"name\":\"Neo\"},null]", result);
    }
    
    @Test
    public void testQueryEndpointMultipleResults() throws Exception {
        Node andres=createNode("name","Andres");
        Node peter=createNode("name","Peter");
        cypherRsPath.put(ClientResponse.class, "match n where id(n) in {ids} return n");
        ClientResponse response = post(map("ids", asList(andres.getId(), peter.getId())));
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals("[{\"name\":\"Andres\"},{\"name\":\"Peter\"}]", result);
    }

    @Test
    public void testQueryEndpointMultipleColumns() throws Exception {
        Node andres=createNode("name","Andres");
        cypherRsPath.put(ClientResponse.class, MULTI_COLUMN_QUERY);
        ClientResponse response = post(map("ids", asList(andres.getId())));
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals("{\"l\":6,\"name\":\"Andres\"}", result);
    }

    @Test
    public void testQueryEndpointMultipleRowsMultipleColumns() throws Exception {
        Node andres=createNode("name","Andres");
        Node peter=createNode("name","Peter");
        cypherRsPath.put(ClientResponse.class, MULTI_COLUMN_QUERY);
        ClientResponse response = post(map("ids", asList(andres.getId(), peter.getId())));
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals("[{\"l\":6,\"name\":\"Andres\"},{\"l\":5,\"name\":\"Peter\"}]", result);
    }
    @Test
    public void testGetQueryWriteQueryShouldReturnOkMethod() throws Exception {
        cypherRsPath.put(ClientResponse.class, WRITE_QUERY);
        ClientResponse response = post(map("name", "foobar"));
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals("{\"name\":\"foobar\"}", result);
    }

    private ClientResponse post(Map<String, Object> payload) throws IOException {
        return cypherRsPath.entity(Utils.toJson(payload), MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class);
    }
}
