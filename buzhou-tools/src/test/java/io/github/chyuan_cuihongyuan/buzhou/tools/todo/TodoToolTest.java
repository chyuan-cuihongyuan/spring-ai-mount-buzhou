package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * todo 工具（ticket 16 验收：写入会话 state、跨实例续接后清单仍在、Attachment 渲染）。
 */
class TodoToolTest {

    private SessionStateStore stateStore;
    private TodoStore todoStore;
    private TodoTool todo;

    @BeforeEach
    void setUp() {
        stateStore = new InMemorySessionStateStore();
        todoStore = new TodoStore(stateStore);
        todo = new TodoTool(todoStore);
    }

    private static ToolContext session(String sessionId) {
        return new ToolContext(Map.of("buzhou.sessionId", sessionId));
    }

    @Test
    void upsertListRemoveClear() {
        String r1 = todo.call("""
                {"action":"upsert","items":[
                  {"id":"t1","content":"修数据库连接池配置","status":"in_progress"},
                  {"id":"t2","content":"补单测","status":"pending"}]}
                """, session("s1"));
        assertThat(r1).contains("新增 2");

        String list = todo.call("{\"action\":\"list\"}", session("s1"));
        assertThat(list).contains("[~]").contains("修数据库连接池配置").contains("[ ]").contains("补单测");

        // upsert 同 id = 更新（保留原创建轮次）
        todo.call("""
                {"action":"upsert","items":[{"id":"t1","content":"修数据库连接池配置","status":"completed"}]}
                """, session("s1"));
        assertThat(todo.call("{\"action\":\"list\"}", session("s1"))).contains("[x]");

        todo.call("{\"action\":\"remove\",\"ids\":[\"t2\"]}", session("s1"));
        assertThat(todo.call("{\"action\":\"list\"}", session("s1")))
                .doesNotContain("补单测").contains("修数据库连接池配置");

        todo.call("{\"action\":\"clear\"}", session("s1"));
        assertThat(todo.call("{\"action\":\"list\"}", session("s1"))).contains("为空");
    }

    @Test
    void persistsAcrossInstances() {
        // 实例 A 写入
        todo.call("""
                {"action":"upsert","items":[{"id":"t1","content":"跨实例任务","status":"pending"}]}
                """, session("s-shared"));

        // 实例 B（新的 TodoTool/TodoStore，同一 SessionStateStore）凭 sessionId 续接
        TodoTool instanceB = new TodoTool(new TodoStore(stateStore));
        String list = instanceB.call("{\"action\":\"list\"}", session("s-shared"));
        assertThat(list).contains("跨实例任务");

        // state 落点校验：key/producer 符合 spec 06 存储 Schema
        assertThat(stateStore.get("s-shared", TodoStore.KEY)).isPresent()
                .get().satisfies(e -> {
                    assertThat(e.producer()).isEqualTo("builtin:todo");
                    assertThat(e.ttlTurns()).isNull();
                    assertThat(e.value()).contains("跨实例任务");
                });
    }

    @Test
    void sessionIsolation() {
        todo.call("""
                {"action":"upsert","items":[{"id":"t1","content":"会话A的任务","status":"pending"}]}
                """, session("s-a"));
        assertThat(todo.call("{\"action\":\"list\"}", session("s-b"))).contains("为空");
    }

    @Test
    void attachmentRendererRendersItems() {
        TodoAttachmentRenderer renderer = new TodoAttachmentRenderer(todoStore);
        assertThat(renderer.render("s1", 3)).isEmpty();

        todo.call("""
                {"action":"upsert","items":[{"id":"t1","content":"待办事项","status":"pending"}]}
                """, session("s1"));
        assertThat(renderer.render("s1", 3)).isPresent()
                .get().asString().contains("任务清单").contains("待办事项");
    }

    @Test
    void removeReportsActualCount() {
        todo.call("""
                {"action":"upsert","items":[{"id":"t1","content":"真实任务","status":"pending"}]}
                """, session("s1"));
        // ids 含不存在项：如实报告实际删除数，不虚报
        String removed = todo.call("{\"action\":\"remove\",\"ids\":[\"t1\",\"ghost\"]}", session("s1"));
        assertThat(removed).contains("已删除 1 项");
        String none = todo.call("{\"action\":\"remove\",\"ids\":[\"ghost\"]}", session("s1"));
        assertThat(none).contains("未删除任何项");
    }

    @Test
    void rejectsOutOfSessionCall() {
        assertThat(todo.call("{\"action\":\"list\"}")).contains("需在 harness 会话内调用");
    }

    @Test
    void invalidActionAndStatusRejected() {
        assertThat(todo.call("{\"action\":\"bogus\"}", session("s1"))).contains("未知 action");
        assertThat(todo.call("""
                {"action":"upsert","items":[{"id":"t1","content":"x","status":"bogus"}]}
                """, session("s1"))).contains("status 非法");
    }
}
