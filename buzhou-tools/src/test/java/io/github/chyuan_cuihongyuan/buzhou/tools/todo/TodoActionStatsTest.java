package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodoActionStatsTest {

    private SessionStateStore stateStore;
    private TodoTool todo;

    @BeforeEach
    void setUp() {
        stateStore = new InMemorySessionStateStore();
        todo = new TodoTool(new TodoStore(stateStore));
    }

    private static ToolContext session(String sessionId) {
        return new ToolContext(Map.of("buzhou.sessionId", sessionId));
    }

    @Test
    void freshToolHasAllZeroBuckets() {
        TodoTool.TodoActionStats stats = todo.actionStats();
        assertThat(stats.total()).isZero();
        assertThat(stats.byAction().get("list")).isZero();
        assertThat(stats.byAction().get("upsert")).isZero();
        assertThat(stats.byAction().get("remove")).isZero();
        assertThat(stats.byAction().get("clear")).isZero();
        assertThat(stats.byAction().get("other")).isZero();
    }

    @Test
    void eachActionCountedIntoItsBucket() {
        todo.call("{\"action\":\"upsert\",\"items\":[{\"id\":\"t1\",\"content\":\"c\",\"status\":\"pending\"}]}",
                session("s1"));
        todo.call("{\"action\":\"list\"}", session("s1"));
        todo.call("{\"action\":\"remove\",\"ids\":[\"t1\"]}", session("s1"));
        todo.call("{\"action\":\"clear\"}", session("s1"));

        TodoTool.TodoActionStats stats = todo.actionStats();
        assertThat(stats.byAction().get("upsert")).isEqualTo(1);
        assertThat(stats.byAction().get("list")).isEqualTo(1);
        assertThat(stats.byAction().get("remove")).isEqualTo(1);
        assertThat(stats.byAction().get("clear")).isEqualTo(1);
        assertThat(stats.total()).isEqualTo(4);
    }

    @Test
    void unknownActionBucketedAsOther() {
        todo.call("{\"action\":\"bogus\"}", session("s1"));

        TodoTool.TodoActionStats stats = todo.actionStats();
        assertThat(stats.byAction().get("other")).isEqualTo(1);
        assertThat(stats.byAction().get("list")).isZero();
    }

    @Test
    void repeatedCallsAccumulate() {
        todo.call("{\"action\":\"list\"}", session("s1"));
        todo.call("{\"action\":\"list\"}", session("s1"));
        todo.call("{\"action\":\"list\"}", session("s1"));

        assertThat(todo.actionStats().byAction().get("list")).isEqualTo(3);
    }

    @Test
    void snapshotIsImmutable() {
        todo.call("{\"action\":\"list\"}", session("s1"));

        var stats = todo.actionStats();
        assertThatThrownBy(() -> stats.byAction().put("x", 1L))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
