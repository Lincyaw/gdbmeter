package ch.ethz.ast.gdblancer.neo4j.oracle;

import ch.ethz.ast.gdblancer.common.ExpectedErrors;
import ch.ethz.ast.gdblancer.common.GlobalState;
import ch.ethz.ast.gdblancer.common.Oracle;
import ch.ethz.ast.gdblancer.neo4j.Neo4JConnection;
import ch.ethz.ast.gdblancer.neo4j.Neo4JQuery;
import ch.ethz.ast.gdblancer.neo4j.gen.ast.Neo4JExpression;
import ch.ethz.ast.gdblancer.neo4j.gen.ast.Neo4JExpressionGenerator;
import ch.ethz.ast.gdblancer.neo4j.gen.ast.Neo4JPrefixOperation;
import ch.ethz.ast.gdblancer.neo4j.gen.ast.Neo4JVisitor;
import ch.ethz.ast.gdblancer.neo4j.gen.schema.Neo4JDBEntity;
import ch.ethz.ast.gdblancer.neo4j.gen.schema.Neo4JDBSchema;
import ch.ethz.ast.gdblancer.neo4j.gen.schema.Neo4JType;
import ch.ethz.ast.gdblancer.neo4j.gen.util.Neo4JDBUtil;
import ch.ethz.ast.gdblancer.util.IgnoreMeException;

import java.util.List;
import java.util.Map;

public class Neo4JNonEmptyResult implements Oracle {

    private final GlobalState<Neo4JConnection> state;
    private final Neo4JDBSchema schema;

    public Neo4JNonEmptyResult(GlobalState<Neo4JConnection> state, Neo4JDBSchema schema) {
        this.state = state;
        this.schema = schema;
    }

    @Override
    public void check() {
        ExpectedErrors errors = new ExpectedErrors();

        Neo4JDBUtil.addRegexErrors(errors);
        Neo4JDBUtil.addArithmeticErrors(errors);
        Neo4JDBUtil.addFunctionErrors(errors);

        String label = schema.getRandomLabel();
        Neo4JDBEntity entity = schema.getEntityByLabel(label);

        StringBuilder query = new StringBuilder();
        query.append(String.format("MATCH (n:%s)", label));
        query.append(" WHERE ");
        Neo4JExpression matchingExpression = Neo4JExpressionGenerator.generateExpression(Map.of("n", entity), Neo4JType.BOOLEAN);
        query.append(Neo4JVisitor.asString(matchingExpression));
        query.append(" RETURN n");

        Neo4JQuery initialQuery = new Neo4JQuery(query.toString(), errors);
        List<Map<String, Object>> initialResult = initialQuery.executeAndGet(state);

        if (initialResult == null) {
            throw new IgnoreMeException();
        }

        int initialSize = initialResult.size();

        if (initialSize != 0) {
            String deletionQuery = String.format("MATCH (n:%s)", label) +
                    " WHERE " +
                    Neo4JVisitor.asString(new Neo4JPrefixOperation(matchingExpression, Neo4JPrefixOperation.PrefixOperator.NOT)) +
                    " DETACH DELETE n";

            new Neo4JQuery(deletionQuery, errors).execute(state);
            List<Map<String, Object>> result = initialQuery.executeAndGet(state);

            if (result == null) {
                throw new AssertionError("Non empty oracle failed because the second query threw an exception");
            }

            if (result.size() != initialSize) {
                throw new AssertionError(String.format("Non empty oracle failed with size: %d, expected was %d", result.size(), initialResult.size()));
            }
        }
    }

}