package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionResourceRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * 会话装配上下文：{@link SessionAssemblyCustomizer} 经此向 ChatClient 注入 advisor、
 * 包装工具回调、读会话资源注册表与 SpanContextCarrier。
 *
 * <p>设计意图：core 的 {@code HarnessAssembler} 暴露固定装配步骤（ToolCallingAdvisor/Memory/Hook
 * 三件套），机制模块（如 buzhou-observability）通过本 SPI 在不反向依赖 core 装配内部的前提下，
 * 注入自身 advisor（如 ObservabilityAdvisor）与工具包装（如 ObservableToolCallback），
 * 保持 core ← 机制模块 的单向依赖。
 */
public interface SessionAssemblyContext {

    String appId();

    String agentName();

    String sessionId();

    BuzhouStores stores();

    SessionResourceRegistry registry();

    /** 本会话的 SpanContextCarrier（per-session，由 assembler 创建）；用于 advisor/工具包装共享。 */
    SpanContextCarrier spanContextCarrier();

    /** 当前已装配的 advisor 清单（含 core 三件套与先前 customizer 注入项）；可继续 add。 */
    List<Advisor> advisors();

    /** 向清单追加 advisor（顺序决定 advisor 链排序）。 */
    void addAdvisor(Advisor advisor);

    /**
     * 注册一个工具回调包装函数，作用于全部已注册工具回调（含 core 装配的 hookedTools 与 autoTools）。
     * 多次注册按注册顺序层叠（后注册包在更外层）。
     */
    void wrapToolCallbacks(UnaryOperator<org.springframework.ai.tool.ToolCallback> wrapper);

    /** 注册会话生命周期观察者（开/关 SESSION span、强制 flush 等）。 */
    void addObserver(SessionObserver observer);
}
