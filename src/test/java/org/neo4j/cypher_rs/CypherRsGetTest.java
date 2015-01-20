package org.neo4j.cypher_rs;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Hunger @since 09.10.13
 */
public class CypherRsGetTest extends RestTestBase {

    public static final String KEY = "foo";
    public static final String MULTI_COLUMN_QUERY = "match n where id(n) in {ids} return length(n.name) as l, n.name as name";
    private WebResource cypherRsPath;
    public static final String QUERY = "start n=node({id}) return n";
    public static final String WRITE_QUERY = "create (n:Node {name:{name}}) return n";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        WebResource path = rootResource.path("test");
        cypherRsPath = path.path(KEY);
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
        ClientResponse response = cypherRsPath.queryParam("id","123").get(ClientResponse.class);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testQueryEndpoint() throws Exception {
        Node node=createNode("foo","bar");
        cypherRsPath.put(ClientResponse.class, QUERY);
        ClientResponse response = cypherRsPath.queryParam("id",String.valueOf(node.getId())).get(ClientResponse.class);
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals("[{\"foo\":\"bar\"}]", result);
    }

    @Test
    public void testQueryEndpointNoResults() throws Exception {
        cypherRsPath.put(ClientResponse.class, MULTI_COLUMN_QUERY);
        ClientResponse response = cypherRsPath
                .queryParam("ids",String.valueOf(-234))
                .queryParam("ids",String.valueOf(-567))
                .get(ClientResponse.class);
        
        assertEquals(204, response.getStatus());
    }
    
    @Test
    public void testQueryEndpointMultipleResults() throws Exception {
        Node andres=createNode("name","Andres");
        Node peter=createNode("name","Peter");
        cypherRsPath.put(ClientResponse.class, "match n where id(n) in {ids} return n");
        ClientResponse response = cypherRsPath
                .queryParam("ids",String.valueOf(andres.getId()))
                .queryParam("ids", String.valueOf(peter.getId()))
                .get(ClientResponse.class);
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals("[{\"name\":\"Andres\"},{\"name\":\"Peter\"}]", result);
    }

    @Test
    public void testQueryEndpointMultipleColumns() throws Exception {
        Node andres=createNode("name","Andres");
        cypherRsPath.put(ClientResponse.class, MULTI_COLUMN_QUERY);
        ClientResponse response = cypherRsPath
                .queryParam("ids",String.valueOf(andres.getId()))
                .get(ClientResponse.class);
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals("[{\"l\":6,\"name\":\"Andres\"}]", result);
    }

    @Test
    public void testQueryEndpointMultipleRowsMultipleColumns() throws Exception {
        Node andres=createNode("name","Andres");
        Node peter=createNode("name","Peter");
        cypherRsPath.put(ClientResponse.class, MULTI_COLUMN_QUERY);
        ClientResponse response = cypherRsPath
                .queryParam("ids",String.valueOf(andres.getId()))
                .queryParam("ids", String.valueOf(peter.getId()))
                .get(ClientResponse.class);
        String result = response.getEntity(String.class);
        assertEquals(result, 200, response.getStatus());
        assertEquals("[{\"l\":6,\"name\":\"Andres\"},{\"l\":5,\"name\":\"Peter\"}]", result);
    }
    @Test
    public void testGetQueryWriteQueryShouldReturnInvalidMethod() throws Exception {
        cypherRsPath.put(ClientResponse.class, WRITE_QUERY);
        ClientResponse response = cypherRsPath.queryParam("name","foobar").get(ClientResponse.class);
        assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
    }
}
