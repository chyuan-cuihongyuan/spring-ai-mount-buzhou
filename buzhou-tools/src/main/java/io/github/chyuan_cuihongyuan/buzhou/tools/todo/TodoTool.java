package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * todo — 会话作用域任务清单（无害，默认开）。
 *
 * <p>操作集（spec 06 推演 #5）：{@code list / upsert / remove / clear}——upsert 增量合并
 * （按 id 存在即更新、否则追加），降低并发写冲突。数据入 SessionStateStore 随会话持久化；
 * 变更经 {@link TodoAttachmentRenderer} 下轮注入前渲染进 prompt（Hook→state→Attachment 闭环同构）。
 *
 * <p>会话上下文经 ToolContext 解析（{@code buzhou.sessionId} + 轮次 carrier）；
 * 非会话内直调返回提示文本，不抛异常。
 */
@BuzhouTool(name = "todo")
public class TodoTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TodoStore store;

    public TodoTool(TodoStore store) {
        this.store = store;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("todo")
                .description("管理会话任务清单：list 查看 / upsert 增量更新 / remove 删除 / clear 清空。"
                        + "清单随会话持久化，变更后下轮自动注入提示词。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "action":{"type":"string","enum":["list","upsert","remove","clear"],
                            "description":"操作类型"},
                          "items":{"type":"array","description":"upsert 时携带：[{id, content, status}]",
                            "items":{"type":"object","properties":{
                              "id":{"type":"string"},
                              "content":{"type":"string"},
                              "status":{"type":"string","enum":["pending","in_progress","completed"]}
                            },"required":["id","content","status"]}},
                          "ids":{"type":"array","description":"remove 时携带",
                            "items":{"type":"string"}}
                        },"required":["action"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String sessionId = HarnessToolCallingManager.sessionIdOf(toolContext);
        if (sessionId == null) {
            return "todo 失败：需在 harness 会话内调用（缺失会话上下文）";
        }
        int currentTurn = currentTurnOf(toolContext);
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String action = args.path("action").asText("");
            return switch (action) {
                case "list" -> render(store.load(sessionId));
                case "upsert" -> upsert(sessionId, args, currentTurn);
                case "remove" -> remove(sessionId, args, currentTurn);
                case "clear" -> {
                    store.clear(sessionId);
                    yield "任务清单已清空";
                }
                default -> "todo 失败：未知 action：" + action + "（支持 list/upsert/remove/clear）";
            };
        } catch (Exception e) {
            return "todo 失败：" + e.getMessage();
        }
    }

    private String upsert(String sessionId, JsonNode args, int currentTurn) {
        if (!args.hasNonNull("items") || !args.path("items").isArray()) {
            return "todo 失败：upsert 需携带 items 数组";
        }
        List<TodoItem> items = new ArrayList<>(store.load(sessionId));
        int added = 0;
        int updated = 0;
        for (JsonNode node : args.path("items")) {
            TodoItem incoming;
            try {
                incoming = new TodoItem(node.path("id").asText(""),
                        node.path("content").asText(""),
                        node.path("status").asText(""), currentTurn);
            } catch (IllegalArgumentException e) {
                return "todo 失败：" + e.getMessage();
            }
            int idx = indexOf(items, incoming.id());
            if (idx >= 0) {
                // 更新保留原创建轮次
                TodoItem old = items.get(idx);
                items.set(idx, new TodoItem(old.id(), incoming.content(), incoming.status(),
                        old.createdTurn()));
                updated++;
            } else {
                items.add(incoming);
                added++;
            }
        }
        store.save(sessionId, items, currentTurn);
        return "已更新任务清单（新增 " + added + "，更新 " + updated + "）\n" + render(items);
    }

    private String remove(String sessionId, JsonNode args, int currentTurn) {
        if (!args.hasNonNull("ids") || !args.path("ids").isArray()) {
            return "todo 失败：remove 需携带 ids 数组";
        }
        List<String> ids = new ArrayList<>();
        args.path("ids").forEach(n -> ids.add(n.asText()));
        List<TodoItem> items = new ArrayList<>(store.load(sessionId));
        int before = items.size();
        boolean changed = items.removeIf(item -> ids.contains(item.id()));
        if (changed) {
            store.save(sessionId, items, currentTurn);
        }
        int removed = before - items.size();
        // 如实报告实际删除数：ids 可能含不存在的 id，虚报会误导模型后续决策
        return (removed == 0 ? "未删除任何项（无匹配 id）" : "已删除 " + removed + " 项")
                + "\n" + render(items);
    }

    /** 渲染清单文本（工具返回与 Attachment 注入共用同一形态）。 */
    static String render(List<TodoItem> items) {
        if (items.isEmpty()) {
            return "任务清单为空";
        }
        StringBuilder sb = new StringBuilder("任务清单：");
        for (TodoItem item : items) {
            String mark = switch (item.status()) {
                case TodoItem.COMPLETED -> "[x]";
                case TodoItem.IN_PROGRESS -> "[~]";
                default -> "[ ]";
            };
            sb.append('\n').append("- ").append(mark).append(' ')
                    .append(item.content()).append("（").append(item.id()).append(')');
        }
        return sb.toString();
    }

    private static int indexOf(List<TodoItem> items, String id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static int currentTurnOf(ToolContext toolContext) {
        if (toolContext != null && toolContext.getContext() != null) {
            Object carrier = toolContext.getContext().get(SpanContextCarrier.KEY);
            if (carrier instanceof SpanContextCarrier c) {
                SpanContext turn = c.snapshotTurn();
                if (turn != null) {
                    return turn.turnSeq();
                }
            }
        }
        return 0;
    }
}
