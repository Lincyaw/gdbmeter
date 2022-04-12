package ch.ethz.ast.gdblancer.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalStateTests {

    private static class MockConnection implements Connection {

        private final Exception exception;

        private MockConnection(Exception exception) {
            this.exception = exception;
        }

        @Override
        public void connect() {}

        @Override
        public void execute(Query query) throws Exception {
            if (exception != null) {
                throw exception;
            }
        }

        @Override
        public void clearDatabase() {}

        @Override
        public void close() {}
    }

    @Test
    void testNoException() {
        GlobalState<MockConnection> state = new GlobalState<>();
        state.setConnection(new MockConnection(null));
        assertTrue(state.execute(new Query("test")));
    }

    @Test
    void testUnexpectedException() {
        GlobalState<MockConnection> state = new GlobalState<>();
        state.setConnection(new MockConnection(new Exception("unexpected")));
        assertThrows(AssertionError.class, () -> state.execute(new Query("test")));
    }

    @Test
    void testExpectedException() {
        GlobalState<MockConnection> state = new GlobalState<>();
        state.setConnection(new MockConnection(new Exception("expected")));
        assertFalse(state.execute(new Query("test", ExpectedErrors.from("expected"))));
    }

}
