package org.neo4j.cypher_rs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.test.TestGraphDatabaseFactory;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author Michael Hunger @since 10.10.13
 */
public class CypherResultRendererTest {

    private GraphDatabaseService db;
    private CypherResultRenderer renderer;
    private Transaction tx;

    @Before
    public void setUp() throws Exception {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        renderer = new CypherResultRenderer();
        tx = db.beginTx();
    }

    @After
    public void tearDown() throws Exception {
        tx.success();
        tx.close();
        db.shutdown();
    }

    @Test
    public void testRenderPrimitives() throws Exception {
        assertEquals(null, renderer.convert((Object) null));
        assertEquals(1, renderer.convert(1));
        assertEquals(1L, renderer.convert(1L));
        assertEquals(1.1, renderer.convert(1.1));
        assertEquals("foobar", renderer.convert("foobar"));
        assertEquals(true, renderer.convert(true));
        assertEquals(false, renderer.convert(false));
    }

    @Test
    public void testRenderRelationship() throws Exception {
        Node peter = createPeter();
        Relationship rel = createRel(peter);
        assertEquals(map("since", "forever"), renderer.convert(rel));
    }

    @Test
    public void testRenderPath() throws Exception {
        Node node = db.createNode();
        Node node2 = db.createNode();
        Relationship rel = createRel(node,node2);
        PathImpl.Builder path = new PathImpl.Builder(node).push(rel);
        assertEquals("[{},{\"since\":\"forever\"},{}]", Utils.toJson(renderer.convert(path.build())));
    }

    private Relationship createRel(Node...nodes) {
        Node node1 = nodes[(nodes.length < 2 ? 0 : 1)];
        Relationship rel = nodes[0].createRelationshipTo(node1, DynamicRelationshipType.withName("KNOWS"));
        rel.setProperty("since", "forever");
        return rel;
    }

    @Test
    public void testRenderNode() throws Exception {
        Node node = createPeter();
        assertEquals("{\"age\":42,\"children\":[\"Kalle\",\"Oskar\"],\"male\":true,\"name\":\"Peter\"}", Utils.toJson(renderer.convert(node)));
    }

    @Test
    public void testRenderCollections() throws Exception {
        Node node = createPeter();
        assertEquals("[1,2,3]", Utils.toJson(renderer.convert(asList(1, 2, 3))));
        assertEquals("[\"a\",\"b\",\"c\"]", Utils.toJson(renderer.convert(asList("a", "b", "c"))));
        assertEquals("[{\"age\":42,\"children\":[\"Kalle\",\"Oskar\"],\"male\":true,\"name\":\"Peter\"}]", Utils.toJson(renderer.convert(asList(node))));
    }

    private Node createPeter() {
        Node node = db.createNode();
        node.setProperty("name", "Peter");
        node.setProperty("age", 42);
        node.setProperty("male", true);
        node.setProperty("children", new String[]{"Kalle", "Oskar"});
        return node;
    }
}
