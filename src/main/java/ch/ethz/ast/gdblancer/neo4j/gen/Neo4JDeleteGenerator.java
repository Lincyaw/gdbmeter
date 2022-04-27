package ch.ethz.ast.gdblancer.neo4j.gen;

import ch.ethz.ast.gdblancer.common.ExpectedErrors;
import ch.ethz.ast.gdblancer.neo4j.Neo4JQuery;
import ch.ethz.ast.gdblancer.neo4j.gen.schema.Neo4JDBEntity;
import ch.ethz.ast.gdblancer.neo4j.gen.schema.Neo4JDBSchema;
import ch.ethz.ast.gdblancer.neo4j.gen.util.Neo4JDBUtil;
import ch.ethz.ast.gdblancer.util.Randomization;

public class Neo4JDeleteGenerator {

    private final Neo4JDBSchema schema;
    private final StringBuilder query = new StringBuilder();
    private final ExpectedErrors errors = new ExpectedErrors();

    public Neo4JDeleteGenerator(Neo4JDBSchema schema) {
        this.schema = schema;
    }

    public static Neo4JQuery deleteNodes(Neo4JDBSchema schema) {
        return new Neo4JDeleteGenerator(schema).generateDelete();
    }

    // TODO: Support DELETE based on conditions
    // TODO: Support DELETE of relationships
    // TODO: Support DELETE of nodes of different labels
    // TODO: Add RETURN clause
    private Neo4JQuery generateDelete() {
        Neo4JDBUtil.addRegexErrors(errors);
        Neo4JDBUtil.addArithmeticErrors(errors);
        Neo4JDBUtil.addFunctionErrors(errors);

        String label = schema.getRandomLabel();
        Neo4JDBEntity entity = schema.getEntityByLabel(label);

        query.append(String.format("MATCH (n:%s ", label));
        query.append(Neo4JPropertyGenerator.generatePropertyQuery(entity));
        query.append(") ");

        if (Randomization.getBoolean()) {
            query.append("DETACH ");
        } else {
            errors.addRegex("Cannot delete node<\\d+>, because it still has relationships. To delete this node, you must first delete its relationships.");
        }

        query.append("DELETE n");

        return new Neo4JQuery(query.toString(), errors);
    }

}