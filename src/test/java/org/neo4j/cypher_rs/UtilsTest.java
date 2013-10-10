package org.neo4j.cypher_rs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Hunger @since 10.10.13
 */
public class UtilsTest {
    @Test
    public void testIsWriteQuery() throws Exception {
        assertEquals(true, Utils.isWriteQuery("create (n) return n"));
    }
}
