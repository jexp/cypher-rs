/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher_rs;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.core.GraphProperties;
import org.neo4j.kernel.impl.core.NodeManager;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public abstract class RestTestBase {

    private static final String HOSTNAME = "localhost";
    private static final int PORT = 7374;
    private static LocalTestServer neoServer;
    public static final String SERVER_ROOT = "http://" + HOSTNAME + ":" + PORT;
    protected static final String SERVER_ROOT_URI = SERVER_ROOT + "/db/data/";

    static {
        initServer();
    }

    protected static Client client;
    protected static WebResource rootResource;

    protected static void initServer() {
        if (neoServer!=null) {
            neoServer.stop();
        }
        neoServer = new LocalTestServer(HOSTNAME,PORT);
    }

    @BeforeClass
    public static void startDb() throws Exception {
        neoServer.start();
        client = Client.create();
        rootResource = client.resource(SERVER_ROOT);
        tryConnect();
    }

    private static void tryConnect() throws InterruptedException {
        int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                ClientResponse response = rootResource.path("db/data/").get(ClientResponse.class);
                assertEquals(200, response.getStatus());
                System.err.println("Successful HTTP connection to "+SERVER_ROOT_URI);
                return;
            } catch (Exception e) {
                System.err.println("Error retrieving ROOT URI " + e.getMessage());
                Thread.sleep(500);
            }
        }
    }

    @Before
    public void setUp() throws URISyntaxException, Exception {
        neoServer.cleanDb();
    }

    @After
    public void tearDown() throws Exception {
    }

    @AfterClass
    public static void shutdownDb() {
        neoServer.stop();
    }

    protected GraphDatabaseService getGraphDatabase() {
    	return neoServer.getGraphDatabase();
    }

    protected GraphProperties properties() {
        GraphDatabaseAPI api = (GraphDatabaseAPI) neoServer.getGraphDatabase();
        return api.getDependencyResolver().resolveDependency(NodeManager.class).getGraphProperties();
    }

    protected Transaction beginTx() {
        return getGraphDatabase().beginTx();
    }
}
