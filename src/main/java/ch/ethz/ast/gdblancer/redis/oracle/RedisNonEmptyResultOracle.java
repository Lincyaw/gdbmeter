package ch.ethz.ast.gdblancer.redis.oracle;

import ch.ethz.ast.gdblancer.common.ExpectedErrors;
import ch.ethz.ast.gdblancer.common.GlobalState;
import ch.ethz.ast.gdblancer.common.Query;
import ch.ethz.ast.gdblancer.cypher.ast.CypherVisitor;
import ch.ethz.ast.gdblancer.cypher.oracle.CypherNonEmptyResultOracle;
import ch.ethz.ast.gdblancer.common.schema.CypherEntity;
import ch.ethz.ast.gdblancer.common.schema.CypherSchema;
import ch.ethz.ast.gdblancer.common.schema.CypherType;
import ch.ethz.ast.gdblancer.redis.RedisConnection;
import ch.ethz.ast.gdblancer.redis.RedisQuery;
import ch.ethz.ast.gdblancer.redis.RedisUtil;
import ch.ethz.ast.gdblancer.redis.ast.RedisExpressionGenerator;

import java.util.Map;

public class RedisNonEmptyResultOracle extends CypherNonEmptyResultOracle<RedisConnection> {

    public RedisNonEmptyResultOracle(GlobalState<RedisConnection> state, CypherSchema schema) {
        super(state, schema);
    }

    @Override
    protected Query<RedisConnection> getIdQuery() {
        return new RedisQuery("MATCH (n) RETURN id(n)");
    }

    @Override
    protected Query<RedisConnection> getInitialQuery(String label, CypherEntity entity) {
        ExpectedErrors errors = new ExpectedErrors();

        RedisUtil.addFunctionErrors(errors);
        RedisUtil.addArithmeticErrors(errors);

        String query = String.format("MATCH (n:%s) WHERE %s RETURN id(n)",
                label,
                CypherVisitor.asString(RedisExpressionGenerator.generateExpression(Map.of("n", entity), CypherType.BOOLEAN)));

        return new RedisQuery(query, errors);
    }

    @Override
    protected Query<RedisConnection> getDeleteQuery(Long id) {
        return new RedisQuery(String.format("MATCH (n) WHERE id(n) = %d DETACH DELETE n", id));
    }

}
