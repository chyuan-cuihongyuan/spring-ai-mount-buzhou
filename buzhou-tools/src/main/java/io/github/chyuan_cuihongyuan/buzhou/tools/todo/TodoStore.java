package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * todo 清单的 state 存取（spec 06 存储 Schema）：key {@code todo.items}、
 * producer {@code builtin:todo}、JSON 值、ttl 持久（跟随会话生命周期）。
 *
 * <p>跨实例续接：SessionStateStore 无本地状态语义，任意实例凭 sessionId 加载即恢复清单。
 */
public class TodoStore {

    public static final String KEY = "todo.items";
    public static final String PRODUCER = "builtin:todo";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<TodoItem>> LIST_TYPE = new TypeReference<>() {
    };

    private final SessionStateStore stateStore;

    public TodoStore(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    /** 读取清单（无 key 或 JSON 损坏均按空清单——state 损坏不炸工具循环）。 */
    public List<TodoItem> load(String sessionId) {
        Optional<StateEntry> entry = stateStore.get(sessionId, KEY);
        if (entry.isEmpty()) {
            return List.of();
        }
        try {
            return new ArrayList<>(MAPPER.readValue(entry.get().value(), LIST_TYPE));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 整体写回（ttl=null 持久；createdTurn = 最近写入轮次记在每条目上）。 */
    public void save(String sessionId, List<TodoItem> items, int currentTurn) {
        try {
            String json = MAPPER.writeValueAsString(items);
            stateStore.put(sessionId, new StateEntry(KEY, json, PRODUCER, currentTurn, null,
                    Instant.now()));
        } catch (Exception e) {
            throw new IllegalStateException("todo 序列化失败", e);
        }
    }

    public void clear(String sessionId) {
        stateStore.delete(sessionId, KEY);
    }
}
