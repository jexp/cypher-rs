package org.neo4j.cypher_rs;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Hunger @since 09.10.13
 */
public class CypherRsTest extends RestTestBase {

    public static final String KEY = "foo";
    private WebResource cypherRsPath;
    public static final String QUERY = "start n=node({id}) return n";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        WebResource path = rootResource.path("test");
        cypherRsPath = path.path(KEY);
    }

    @Test
    public void testAddEndpoint() throws Exception {
        ClientResponse response = cypherRsPath.put(ClientResponse.class, QUERY);
        assertEquals(201, response.getStatus());
        assertEquals(cypherRsPath.getURI(),response.getLocation());
        Transaction tx = beginTx();
        try {
            assertEquals(QUERY, properties().getProperty(KEY));
            tx.success();
        } finally {
            tx.finish();
        }
    }
    @Test
    public void testDeleteNonExistingEndpoint() throws Exception {
        ClientResponse response = cypherRsPath.delete(ClientResponse.class);
        assertEquals(404, response.getStatus());
    }
    @Test
    public void testDeleteExistingEndpoint() throws Exception {
        cypherRsPath.put(ClientResponse.class, QUERY);
        ClientResponse response = cypherRsPath.delete(ClientResponse.class);
        assertEquals(200, response.getStatus());
    }
}
