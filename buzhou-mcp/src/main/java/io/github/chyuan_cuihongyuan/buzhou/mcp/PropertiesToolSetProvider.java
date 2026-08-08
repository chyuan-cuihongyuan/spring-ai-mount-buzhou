package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.Durations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 静态 properties 清单源（spec 04）：读 {@code buzhou.mcp.servers.*}，启动期一次性解析，
 * 不推送变更（{@link #addChangeListener} 登记即满足契约，永不触发）。
 *
 * <p>yml 形态：
 * <pre>{@code
 * buzhou.mcp.servers:
 *   github:
 *     transport: STREAMABLE_HTTP
 *     endpoint: https://mcp.example.com/github
 *     connect-timeout: 10s
 *     request-timeout: 60s
 *     env: { "Authorization": "${GITHUB_TOKEN}" }
 *     bindings: [ { appId: demo, agentName: triage } ]
 * }</pre>
 */
public class PropertiesToolSetProvider implements ToolSetProvider {

    private final List<ToolSetSpec> specs;

    public PropertiesToolSetProvider(List<ToolSetSpec> specs) {
        this.specs = List.copyOf(specs);
    }

    /** 从 yml map（{@code buzhou.mcp.servers} 的值，name → 连接描述 map）解析。 */
    @SuppressWarnings("unchecked")
    public static PropertiesToolSetProvider fromServersMap(Map<String, Object> servers) {
        List<ToolSetSpec> specs = new ArrayList<>();
        if (servers != null) {
            for (Map.Entry<String, Object> e : servers.entrySet()) {
                if (!(e.getValue() instanceof Map<?, ?> raw)) {
                    throw new IllegalArgumentException(
                            "buzhou.mcp.servers." + e.getKey() + " must be a map");
                }
                specs.add(parseSpec(e.getKey(), (Map<String, Object>) raw));
            }
        }
        return new PropertiesToolSetProvider(specs);
    }

    private static ToolSetSpec parseSpec(String name, Map<String, Object> map) {
        Transport transport = Transport.valueOf(strVal(map, "transport", "STREAMABLE_HTTP"));
        String endpoint = strVal(map, "endpoint", null);
        Map<String, String> env = new LinkedHashMap<>();
        if (map.get("env") instanceof Map<?, ?> envMap) {
            envMap.forEach((k, v) -> env.put(String.valueOf(k), String.valueOf(v)));
        }
        Set<ToolSetSpec.Binding> bindings = new LinkedHashSet<>();
        if (map.get("bindings") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> b) {
                    bindings.add(new ToolSetSpec.Binding(
                            String.valueOf(b.get("appId")), String.valueOf(b.get("agentName"))));
                }
            }
        }
        return new ToolSetSpec(name, transport, endpoint, env,
                durationVal(map, "connect-timeout"), durationVal(map, "request-timeout"), bindings);
    }

    private static String strVal(Map<String, Object> map, String key, String defaultValue) {
        Object v = map.get(key);
        return v == null ? defaultValue : String.valueOf(v);
    }

    /** 时长解析：支持 Duration 直传、ISO-8601（PT30S）、毫秒数与 30s/500ms 后缀串。 */
    static Duration durationVal(Map<String, Object> map, String key) {
        return Durations.fromMap(map, key);
    }

    static Duration parseDuration(String text) {
        return Durations.parse(text);
    }

    @Override
    public List<ToolSetSpec> currentToolSets() {
        return specs;
    }

    @Override
    public void addChangeListener(Runnable onChange) {
        // 静态源：properties 不变更不推送；登记进实例列表仅为满足契约（调用方遍历时不 NPE）
        listeners.add(onChange);
    }

    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
}
