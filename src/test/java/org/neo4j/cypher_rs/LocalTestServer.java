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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.core.GraphPropertiesImpl;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.logging.DevNullLoggingService;
import org.neo4j.kernel.logging.Logging;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.configuration.PropertyFileConfigurator;
import org.neo4j.server.database.Database;
import org.neo4j.server.database.WrappedDatabase;
import org.neo4j.server.modules.RESTApiModule;
import org.neo4j.server.modules.ServerModule;
import org.neo4j.server.modules.ThirdPartyJAXRSModule;
import org.neo4j.server.preflight.PreFlightTasks;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

/**
 * @author mh
 * @since 24.03.11
 */
public class LocalTestServer {
    private CommunityNeoServer neoServer;
    private final int port;
    private final String hostname;
    protected String propertiesFile = "test-db.properties";
    private final ImpermanentGraphDatabase graphDatabase;

    public LocalTestServer() {
        this("localhost",7473);
    }

    public LocalTestServer(String hostname, int port) {
        this.port = port;
        this.hostname = hostname;
        graphDatabase = (ImpermanentGraphDatabase) new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    public void start() {
        if (neoServer!=null) throw new IllegalStateException("Server already running");
        URL url = getClass().getResource("/" + propertiesFile);
        if (url==null) throw new IllegalArgumentException("Could not resolve properties file "+propertiesFile);
        Logging logging = new DevNullLoggingService();
        final PropertyFileConfigurator configurator = new PropertyFileConfigurator(new File(url.getPath()));
        final LocalTestDbFactory dbFactory = new LocalTestDbFactory(new WrappedDatabase(graphDatabase));
        neoServer = new CommunityNeoServer(configurator, dbFactory, logging) {

            @Override
            protected int getWebServerPort() {
                return port;
            }

            @Override
            protected PreFlightTasks createPreflightTasks() {
                return new PreFlightTasks(logging);
            }

            @Override
            protected Iterable<ServerModule> createServerModules() {
                return Arrays.asList(
                        new RESTApiModule(webServer, database, configurator.configuration(), logging),
                        new ThirdPartyJAXRSModule(webServer, configurator, logging, this));
            }
        };
        neoServer.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
        neoServer.stop();
        } catch(Exception e) {
            System.err.println("Error stopping server: "+e.getMessage());
        }
        neoServer=null;
    }

    public int getPort() {
        return port;
    }

    public String getHostname() {
        return hostname;
    }

    public LocalTestServer withPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
        return this;
    }
    public Database getDatabase() {
        return neoServer.getDatabase();
    }

    public URI baseUri() {
        return neoServer.baseUri();
    }

    public void cleanDb() {
        graphDatabase.cleanContent();
        cleanGraphProperties();
    }

    private void cleanGraphProperties() {
        try (Transaction tx = graphDatabase.beginTx()) {
            GraphPropertiesImpl props = graphDatabase.getDependencyResolver().resolveDependency(NodeManager.class).getGraphProperties();
            for (String key : asCollection(props.getPropertyKeys())) {
                props.removeProperty(key);
            }
            tx.success();
        }
    }

    private static class LocalTestDbFactory implements Database.Factory {
        private final Database db;

        private LocalTestDbFactory(Database db) {
            this.db = db;
        }

        @Override
        public Database newDatabase(final Config config, final Logging logging) {
            return db;
        }
    }

    public GraphDatabaseService getGraphDatabase() {
        return graphDatabase;
    }
}
