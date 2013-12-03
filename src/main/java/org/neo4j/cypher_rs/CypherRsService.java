package org.neo4j.cypher_rs;


import au.com.bytecode.opencsv.CSVReader;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.cypher.javacompat.QueryStatistics;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.core.GraphProperties;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.server.database.CypherExecutor;
import org.neo4j.server.database.Database;
import org.neo4j.server.rest.repr.BadInputException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

/**
 * @author Michael Hunger @since 09.10.13
 */


@Path("/")
public class CypherRsService {

    private final ExecutionEngine engine;
    private final GraphDatabaseAPI db;
    private final GraphProperties props;

    public CypherRsService(@Context CypherExecutor executor, @Context Database database) {
        engine = executor.getExecutionEngine();
        db = database.getGraph();
        props = db.getDependencyResolver().resolveDependency(NodeManager.class).getGraphProperties();
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createEndpoint(@PathParam("key") String key, String body, @Context UriInfo uriInfo) {
        try (Transaction tx = db.beginTx()) {
            props.setProperty(key, body);
            tx.success();
            return Response.created(uriInfo.getRequestUri()).build();
        }
    }

    @DELETE
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeEndpoint(@PathParam("key") String key) {
        try (Transaction tx = db.beginTx()) {
            if (props.hasProperty(key)) {
                props.removeProperty(key);
                tx.success();
                return Response.ok().build();
            }
        }
        return notFound();
    }

    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readEndpoint(@PathParam("key") String key, @Context UriInfo uriInfo) {
        try (Transaction tx = db.beginTx()) {
            if (props.hasProperty(key)) {
                String query = (String) props.getProperty(key);
                if (Utils.isWriteQuery(query)) return Response.status(Response.Status.NOT_ACCEPTABLE).build();
                Map<String, Object> params = Utils.toParams(uriInfo.getQueryParameters());
                ExecutionResult result = engine.execute(query, params);
                String json = Utils.toJson(result);
                
                tx.success();
                
                if(json == null)
                    return noContent();
                
                return Response.ok(json).build();
            }
        } catch(Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
        return notFound();
    }

    @POST
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeEndpoint(@PathParam("key") String key, String body) {
        try (Transaction tx = db.beginTx()) {
            if (props.hasProperty(key)) {
                List<Map<String, Object>> params = Utils.toParams(body);
                List<Object> results=new ArrayList<>();
                String query = (String) props.getProperty(key);
                for (Map<String, Object> param : params) {
                    ExecutionResult result = engine.execute(query, param);
                    results.add(Utils.toObject(result));
                }
                tx.success();
                
                Object retVal = singleOrList(results);
                if(retVal == null)
                    return noContent();
                
                return Response.ok(Utils.toJson(retVal)).build();
            }
        } catch (BadInputException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
        return notFound();
    }

    private Object singleOrList(List<Object> results) {
        if (results.size() == 1) return results.get(0);
        return results;
    }

    @POST
    @Path("/{key}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeCsvEndpoint(@PathParam("key") String key, Reader body, @QueryParam("delim") String delim, @QueryParam("batch") String batch) {
        int batchSize = 30000, count = 0;
        if (batch!=null) batchSize = Integer.parseInt(batch);
        Transaction tx = db.beginTx();
        try {
            if (props.hasProperty(key)) {
                String query = (String) props.getProperty(key);
                if (delim==null) delim=",";
                CSVReader reader = new CSVReader(body, delim.charAt(0),'"','\\',0,false,false);
                Map<String,Integer> stats = toMap(0,"nodes_created","nodes_deleted","relationships_created","relationships_deleted","labels_added","labels_removed","properties_set","rows");
                Map<String,Object> header= toMap(null,reader.readNext());
                for (String[] row = reader.readNext(); row != null; row = reader.readNext()) {
                    ExecutionResult result = engine.execute(query, toParams(header,row));
                    accumulateStats(stats, result);
                    if (++count % batchSize == 0) {
                        tx.success();tx.close(); tx = db.beginTx();
                    }
                }
                tx.success();
                return Response.ok(Utils.toJson(stats)).build();
            }
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        } finally {
            tx.close();
            close(body);
        }
        return notFound();
    }

    private void close(Reader reader) {
        try {
            reader.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private void accumulateStats(Map<String, Integer> data, ExecutionResult result) {
        QueryStatistics stats = result.getQueryStatistics();
        if (stats==null || !stats.containsUpdates()) return;
        // "nodes_created","nodes_deleted","relationships_created","relationships_deleted",
        // "labels_added","labels_removed","properties_set","rows"

        add(data, stats.getNodesCreated(), stats.getDeletedNodes(), stats.getRelationshipsCreated(), stats.getDeletedRelationships(),
                stats.getLabelsAdded(), stats.getLabelsRemoved(), stats.getPropertiesSet(), IteratorUtil.count(result));
    }

    private <T> Map<String, T> toMap(T value,String...row) {
        Map<String, T> result = new LinkedHashMap<>(row.length);
        for (String field : row) {
            result.put(field,value);
        }
        return result;
    }

    private Map<String, Object> toParams(Map<String, Object> params, String[] row) {
        int i=0;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            entry.setValue(Utils.convertIfNeeded(row[i++]));
        }
        return params;
    }
    private void add(Map<String, Integer> data, int...stats) {
        int i=0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            entry.setValue(entry.getValue() + stats[i++]);
        }
    }

    private Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    private Response noContent() {
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
