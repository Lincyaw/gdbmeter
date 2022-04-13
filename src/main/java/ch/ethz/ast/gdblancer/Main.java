package ch.ethz.ast.gdblancer;

import ch.ethz.ast.gdblancer.common.GlobalState;
import ch.ethz.ast.gdblancer.neo4j.Neo4JConnection;
import ch.ethz.ast.gdblancer.neo4j.Neo4JGenerator;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        GlobalState<Neo4JConnection> state = new GlobalState<>();

        while (true) {
            try (Neo4JConnection connection = new Neo4JConnection()) {
                connection.connect();
                state.setConnection(connection);
                new Neo4JGenerator().generate(state);
            } finally {
                state.getLogger().info("Finished iteration, closing database");
            }
        }
    }

}
