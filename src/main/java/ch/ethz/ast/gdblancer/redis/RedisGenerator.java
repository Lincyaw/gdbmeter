package ch.ethz.ast.gdblancer.redis;

import ch.ethz.ast.gdblancer.common.GlobalState;
import ch.ethz.ast.gdblancer.cypher.schema.CypherSchema;
import ch.ethz.ast.gdblancer.redis.gen.RedisCreateGenerator;
import ch.ethz.ast.gdblancer.redis.gen.RedisCreateIndexGenerator;
import ch.ethz.ast.gdblancer.redis.gen.RedisRemoveGenerator;
import ch.ethz.ast.gdblancer.util.IgnoreMeException;
import ch.ethz.ast.gdblancer.util.Randomization;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RedisGenerator {

    enum Action {
        CREATE(RedisCreateGenerator::createEntities),
        CREATE_INDEX(RedisCreateIndexGenerator::createIndex),
        REMOVE(RedisRemoveGenerator::removeProperties);

        private final Function<CypherSchema, RedisQuery> generator;

        Action(Function<CypherSchema, RedisQuery> generator) {
            this.generator = generator;
        }
    }

    private static int mapAction(Action action) {
        int selectedNumber = 0;

        switch (action) {
            case CREATE:
                selectedNumber = Randomization.nextInt(50, 70);
                break;
            case CREATE_INDEX:
                selectedNumber = Randomization.nextInt(3,  10);
                break;
            case REMOVE:
                selectedNumber = Randomization.nextInt(0, 8);
                break;
            default:
                throw new AssertionError(action);
        }

        return selectedNumber;
    }

    private final CypherSchema schema;

    public RedisGenerator(CypherSchema schema) {
        this.schema = schema;
    }

    public void generate(GlobalState<RedisConnection> globalState) {
        List<Function<CypherSchema, RedisQuery>> queries = new ArrayList<>();

        // Sample the actions
        for (Action action : Action.values()) {
            int amount = mapAction(action);

            for (int i = 0; i < amount; i++) {
                queries.add(action.generator);
            }
        }

        Randomization.shuffleList(queries);

        for (Function<CypherSchema, RedisQuery> queryGenerator : queries) {
            try {
                int tries = 0;
                boolean success;
                RedisQuery query;

                do {
                    query = queryGenerator.apply(schema);
                    success = query.execute(globalState);
                } while (!success && tries++ < 1000);

                if (success && query.couldAffectSchema()) {
                    // TODO: Move to global state later
                    schema.setIndices(globalState.getConnection().getIndexNames());
                }
            } catch (IgnoreMeException ignored) {}
        }

    }

}
