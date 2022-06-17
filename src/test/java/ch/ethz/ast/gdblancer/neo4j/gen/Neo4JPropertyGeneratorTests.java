package ch.ethz.ast.gdblancer.neo4j.gen;

import ch.ethz.ast.gdblancer.cypher.schema.CypherEntity;
import ch.ethz.ast.gdblancer.cypher.schema.CypherType;
import ch.ethz.ast.gdblancer.util.IgnoreMeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Neo4JPropertyGeneratorTests {

    @Test
    void testCreatePropertyQuery() {
        try {
            CypherEntity entity = CypherEntity.generateRandomEntity(CypherType.values());

            String query = new Neo4JPropertyGenerator(entity).generateProperties();
            assertNotNull(query);
        } catch (IgnoreMeException ignored) {

        }
    }

}
