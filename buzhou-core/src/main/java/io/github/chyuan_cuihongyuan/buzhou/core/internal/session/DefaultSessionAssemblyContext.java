package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class DefaultSessionAssemblyContext implements SessionAssemblyContext {

    private final String appId;
    private final String agentName;
    private final String sessionId;
    private final BuzhouStores stores;
    private final SessionResourceRegistry registry;
    private final SpanContextCarrier spanContextCarrier;
    private final Consumer<SessionEvent> eventEmitter;
    private final List<Advisor> advisors = new ArrayList<>();
    private final List<UnaryOperator<ToolCallback>> toolWrappers = new ArrayList<>();
    private final List<ToolCallback> extraTools = new ArrayList<>();
    private final List<SessionObserver> observers = new ArrayList<>();

    public DefaultSessionAssemblyContext(String appId, String agentName, String sessionId,
                                         BuzhouStores stores, SessionResourceRegistry registry,
                                         SpanContextCarrier spanContextCarrier,
                                         Consumer<SessionEvent> eventEmitter) {
        this.appId = appId;
        this.agentName = agentName;
        this.sessionId = sessionId;
        this.stores = stores;
        this.registry = registry;
        this.spanContextCarrier = spanContextCarrier;
        this.eventEmitter = eventEmitter == null ? event -> {
        } : eventEmitter;
    }

    @Override
    public String appId() {
        return appId;
    }

    @Override
    public String agentName() {
        return agentName;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public BuzhouStores stores() {
        return stores;
    }

    @Override
    public SessionResourceRegistry registry() {
        return registry;
    }

    @Override
    public SpanContextCarrier spanContextCarrier() {
        return spanContextCarrier;
    }

    @Override
    public List<Advisor> advisors() {
        return advisors;
    }

    @Override
    public void addAdvisor(Advisor advisor) {
        advisors.add(advisor);
    }

    @Override
    public void wrapToolCallbacks(UnaryOperator<ToolCallback> wrapper) {
        toolWrappers.add(wrapper);
    }

    @Override
    public void addToolCallbacks(List<ToolCallback> tools) {
        if (tools != null && !tools.isEmpty()) {
            extraTools.addAll(tools);
        }
    }

    public List<UnaryOperator<ToolCallback>> toolWrappers() {
        return toolWrappers;
    }

    /** customizer 经 {@link #addToolCallbacks} 注入的新工具（MCP 等动态工具集）。 */
    public List<ToolCallback> extraTools() {
        return extraTools;
    }

    @Override
    public void addObserver(SessionObserver observer) {
        observers.add(observer);
    }

    public List<SessionObserver> observers() {
        return observers;
    }

    @Override
    public void emitEvent(SessionEvent event) {
        eventEmitter.accept(event);
    }
}
