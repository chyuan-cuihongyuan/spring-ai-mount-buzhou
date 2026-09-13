package io.github.chyuan_cuihongyuan.buzhou.mcp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 建连遥测读数（spec 840 / T1181，gRPC channelz 连接遥测思想——823
 * keepalive 借鉴的姊妹面）：per-server 建连成败计数+建连耗时近窗环+连续
 * 失败 streak——「server 连不上还是连上慢」从连接日志变样本面（与 822
 * 能力快照正交：那是建连后内容、这是建连本身）。
 *
 * <p>纯读数：server 封顶 {@value #MAX_SERVERS}（超限 truncated 不记）；
 * per-server 环 {@value #RING}；record(null/空白/负耗时) 忽略。喂点=工厂
 * /注册表装配侧。
 */
public final class McpConnectTelemetry {

    /** server 数封顶。 */
    public static final int MAX_SERVERS = 32;
    /** 耗时样本环容量。 */
    public static final int RING = 16;

    /** 单 server 遥测行。 */
    public record ServerTelemetry(String server, long successes, long failures,
                                  int consecutiveFailures, long lastDurationMillis,
                                  double recentSuccessRate) {
    }

    private static final class State {
        final Deque<Boolean> window = new ArrayDeque<>(RING);
        long successes;
        long failures;
        int consecutiveFailures;
        long lastDuration = -1;
    }

    private final Map<String, State> servers = new LinkedHashMap<>();
    private volatile boolean truncated;

    /** 记录一次建连结果（durationMillis 负值=未知记 0；null/空白 server 忽略）。 */
    public void record(String server, boolean success, long durationMillis) {
        if (server == null || server.isBlank()) {
            return;
        }
        State state;
        synchronized (servers) {
            state = servers.get(server);
            if (state == null) {
                if (servers.size() >= MAX_SERVERS) {
                    truncated = true;
                    return;
                }
                state = new State();
                servers.put(server, state);
            }
        }
        synchronized (state) {
            if (success) {
                state.successes++;
                state.consecutiveFailures = 0;
            } else {
                state.failures++;
                state.consecutiveFailures++;
            }
            if (durationMillis >= 0) {
                state.lastDuration = durationMillis;
            }
            if (state.window.size() >= RING) {
                state.window.pollFirst();
            }
            state.window.addLast(success);
        }
    }

    /** 单 server 读数（未知 null）。 */
    public ServerTelemetry stats(String server) {
        State state = server == null ? null : servers.get(server);
        if (state == null) {
            return null;
        }
        synchronized (state) {
            long ok = 0;
            for (Boolean b : state.window) {
                if (b) {
                    ok++;
                }
            }
            double rate = state.window.isEmpty() ? 0 : (double) ok / state.window.size();
            return new ServerTelemetry(server, state.successes, state.failures,
                    state.consecutiveFailures, state.lastDuration, rate);
        }
    }

    /** 全 server 读数（按失败数降序——问题 server 排前）。 */
    public List<ServerTelemetry> worstFirst() {
        List<ServerTelemetry> all = new ArrayList<>();
        for (String server : servers.keySet()) {
            ServerTelemetry s = stats(server);
            if (s != null) {
                all.add(s);
            }
        }
        all.sort(java.util.Comparator.comparingLong(ServerTelemetry::failures).reversed()
                .thenComparing(ServerTelemetry::server));
        return List.copyOf(all);
    }

    public boolean truncated() {
        return truncated;
    }
}
