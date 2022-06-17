package ch.ethz.ast.gdblancer.redis.oracle;

import ch.ethz.ast.gdblancer.common.ExpectedErrors;
import ch.ethz.ast.gdblancer.common.GlobalState;
import ch.ethz.ast.gdblancer.common.Oracle;
import ch.ethz.ast.gdblancer.cypher.ast.*;
import ch.ethz.ast.gdblancer.cypher.schema.CypherEntity;
import ch.ethz.ast.gdblancer.cypher.schema.CypherSchema;
import ch.ethz.ast.gdblancer.cypher.schema.CypherType;
import ch.ethz.ast.gdblancer.redis.RedisConnection;
import ch.ethz.ast.gdblancer.redis.RedisQuery;
import ch.ethz.ast.gdblancer.redis.RedisUtil;
import ch.ethz.ast.gdblancer.redis.ast.RedisExpressionGenerator;
import ch.ethz.ast.gdblancer.redis.ast.RedisPointConstant;
import ch.ethz.ast.gdblancer.util.Randomization;
import redis.clients.jedis.graph.entities.Node;
import redis.clients.jedis.graph.entities.Point;
import redis.clients.jedis.graph.entities.Property;

import java.time.LocalDate;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RedisRefinementOracle implements Oracle {

    private final GlobalState<RedisConnection> state;
    private final CypherSchema schema;

    public RedisRefinementOracle(GlobalState<RedisConnection> state, CypherSchema schema) {
        this.state = state;
        this.schema = schema;
    }

    @Override
    public void check() {
        int amount = Randomization.nextInt(2, 5);
        List<String> labels = new ArrayList<>(10);
        CypherExpression whereExpression = new CypherConstant.BooleanConstant(true);

        ExpectedErrors errors = new ExpectedErrors();

        RedisUtil.addFunctionErrors(errors);
        RedisUtil.addArithmeticErrors(errors);

        for (int i = 0; i < amount; i++) {
            labels.add(schema.getRandomLabel());
        }

        // Create match query
        StringBuilder query = new StringBuilder("MATCH ");
        String separator = "";

        for (int i = 0; i < amount; i++) {
            String label = labels.get(i);
            CypherEntity entity = schema.getEntityByLabel(label);

            query.append(separator);
            query.append(String.format("(n%d:%s)", i, label));
            separator = ", ";
            CypherExpression expression = RedisExpressionGenerator.generateExpression(Map.of(String.format("n%d", i), entity), CypherType.BOOLEAN);
            whereExpression = new CypherBinaryLogicalOperation(whereExpression, expression, CypherBinaryLogicalOperation.BinaryLogicalOperator.AND);
        }

        query.append(" WHERE ");
        query.append(CypherVisitor.asString(whereExpression));
        query.append(" RETURN ");

        separator = "";
        for (int i = 0; i < amount; i++) {
            query.append(separator);
            query.append(String.format("n%d", i));
            separator = ", ";
        }

        List<Map<String, Object>> results = new RedisQuery(query.toString(), errors).executeAndGet(state);

        if (results == null) {
            return;
        }

        if (results.isEmpty()) {
            return;
        }

        Map<String, Object> pivotResult = results.get(0);
        List<Long> expectedIds = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            Node node = (Node) pivotResult.get(String.format("n%d", i));
            expectedIds.add(node.getId());
        }

        CypherExpression refinedWhere = new CypherConstant.BooleanConstant(true);

        for (int current = 0; current < amount; current++) {
            if (!verify(labels, expectedIds, refinedWhere)) {
                throw new AssertionError("Not present");
            } else {
                // refine current
                Node node = (Node) pivotResult.get(String.format("n%d", current));
                String label = labels.get(current);
                CypherEntity entity = schema.getEntityByLabel(label);
                String key = Randomization.fromSet(entity.getAvailableProperties().keySet());
                CypherType type = entity.getAvailableProperties().get(key);

                Property<?> property = node.getProperty(key);
                CypherExpression expectedConstant;

                if (property == null) {
                    CypherExpression branch = new CypherPostfixOperation(new CypherVariablePropertyAccess(String.format("n%d.%s", current, key)), CypherPostfixOperation.PostfixOperator.IS_NULL);
                    refinedWhere = new CypherBinaryLogicalOperation(refinedWhere, branch, CypherBinaryLogicalOperation.BinaryLogicalOperator.AND);
                } else {
                    Object value = property.getValue();

                    switch (type) {
                        case BOOLEAN:
                            expectedConstant = new CypherConstant.BooleanConstant((Boolean) value);
                            break;
                        case FLOAT:
                            expectedConstant = new CypherConstant.FloatConstant((Double) value);
                            break;
                        case INTEGER:
                            expectedConstant = new CypherConstant.IntegerConstant((Long) value);
                            break;
                        case STRING:
                            expectedConstant = new CypherConstant.StringConstant((String) value);
                            break;
                        case DATE:
                            LocalDate refinedDate = (LocalDate) value;
                            expectedConstant = new CypherConstant.DateConstant(false, refinedDate.getYear(), refinedDate.getMonthValue(), refinedDate.getDayOfMonth());
                            break;
                        case LOCAL_TIME:
                            OffsetTime refinedTime = (OffsetTime) value;
                            expectedConstant = new CypherConstant.LocalTimeConstant(refinedTime.getHour(), "", refinedTime.getMinute(), refinedTime.getSecond(), ",", refinedTime.getNano());
                            break;
                        case POINT:
                            Point point = (Point) value;
                            expectedConstant = new RedisPointConstant(point.getLongitude(), point.getLatitude());
                            break;
                        default:
                            throw new AssertionError(type);
                    }

                    CypherExpression branch = new CypherBinaryComparisonOperation(new CypherVariablePropertyAccess(String.format("n%d.%s", current, key)), expectedConstant, CypherBinaryComparisonOperation.BinaryComparisonOperator.EQUALS);
                    refinedWhere = new CypherBinaryLogicalOperation(refinedWhere, branch, CypherBinaryLogicalOperation.BinaryLogicalOperator.AND);
                }
            }
        }
    }

    private boolean verify(List<String> labels, List<Long> expectedIds, CypherExpression whereCondition) {
        ExpectedErrors errors = new ExpectedErrors();

        RedisUtil.addFunctionErrors(errors);
        RedisUtil.addArithmeticErrors(errors);

        StringBuilder refinedQuery = new StringBuilder("MATCH ");
        String separator = "";

        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);

            refinedQuery.append(separator);
            refinedQuery.append(String.format("(n%d:%s)", i, label));
            separator = ", ";
        }

        refinedQuery.append(" WHERE ");
        refinedQuery.append(CypherVisitor.asString(whereCondition));
        refinedQuery.append(" RETURN ");

        separator = "";
        for (int i = 0; i < labels.size(); i++) {
            refinedQuery.append(separator);
            refinedQuery.append(String.format("id(n%d)", i));
            separator = ", ";
        }

        List<Map<String, Object>> currentResults = new RedisQuery(refinedQuery.toString(), errors).executeAndGet(state);

        for (Map<String, Object> result : currentResults) {
            for (int i = 0; i < labels.size(); i++) {
                Long id = (Long) result.get(String.format("id(n%d)", i));
                Long expectedId = expectedIds.get(i);

                if (!Objects.equals(id, expectedId)) {
                    break;
                }
            }

            return true;
        }

        return false;
    }

}
