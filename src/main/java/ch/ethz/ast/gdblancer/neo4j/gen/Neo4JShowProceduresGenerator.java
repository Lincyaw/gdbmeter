package ch.ethz.ast.gdblancer.neo4j.gen;

import ch.ethz.ast.gdblancer.neo4j.Neo4JQuery;
import ch.ethz.ast.gdblancer.common.schema.Schema;
import ch.ethz.ast.gdblancer.neo4j.schema.Neo4JType;
import ch.ethz.ast.gdblancer.util.Randomization;

public class Neo4JShowProceduresGenerator {

    private final StringBuilder query = new StringBuilder();

    public static Neo4JQuery showProcedures(Schema<Neo4JType> ignored) {
        return new Neo4JShowProceduresGenerator().generateShowProcedures();
    }

    private Neo4JQuery generateShowProcedures() {
        query.append("SHOW PROCEDURE");

        if (Randomization.getBoolean()) {
            query.append("S");
        }

        return new Neo4JQuery(query.toString());
    }

}
