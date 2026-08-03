package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Event 类型：开放枚举（字符串值 + 注册表），非 Java enum（spec 03 推演 1）。
 *
 * <p>核心五类为内置常量；框架扩展事件内置注册；业务可经 {@link #of(String)} 注册自定义类型。
 * 注册表形态保留开放语义且序列化天然稳定（存字符串值）。
 */
public final class EventType {

    // 核心五类
    public static final String THINKING = "THINKING";
    public static final String FINAL_REPLY = "FINAL_REPLY";
    public static final String TOOL_INPUT = "TOOL_INPUT";
    public static final String TOOL_OUTPUT = "TOOL_OUTPUT";
    public static final String ERROR = "ERROR";
    // 框架扩展事件（衔接 ticket 10/24/25 因果串联）
    public static final String DANGLING_REPAIR = "DANGLING_REPAIR";
    public static final String HITL_REQUEST = "HITL_REQUEST";
    public static final String HITL_DECISION = "HITL_DECISION";
    public static final String GUARD_ACTION = "GUARD_ACTION";

    private static final ConcurrentHashMap<String, String> REGISTRY = new ConcurrentHashMap<>();

    static {
        REGISTRY.put(THINKING, THINKING);
        REGISTRY.put(FINAL_REPLY, FINAL_REPLY);
        REGISTRY.put(TOOL_INPUT, TOOL_INPUT);
        REGISTRY.put(TOOL_OUTPUT, TOOL_OUTPUT);
        REGISTRY.put(ERROR, ERROR);
        REGISTRY.put(DANGLING_REPAIR, DANGLING_REPAIR);
        REGISTRY.put(HITL_REQUEST, HITL_REQUEST);
        REGISTRY.put(HITL_DECISION, HITL_DECISION);
        REGISTRY.put(GUARD_ACTION, GUARD_ACTION);
    }

    /**
     * lookup-or-create：内置类型直接返回，新值经 computeIfAbsent 注册后返回原值。
     *
     * @return 归一化后的类型字符串（与入参一致）
     */
    public static String of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EventType must be non-blank");
        }
        return REGISTRY.computeIfAbsent(value, k -> value);
    }

    private EventType() {
    }
}
