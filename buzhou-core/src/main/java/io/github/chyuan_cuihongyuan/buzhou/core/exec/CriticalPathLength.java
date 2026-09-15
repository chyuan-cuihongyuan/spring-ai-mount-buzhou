package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关键路径长度（spec 1867 / T2935 / impl 1468）——项目管理 CPM（关键
 * 路径法）思想：并行任务 DAG 的**最长加权路径**即总时长下界——缩短非
 * 关键任务不缩总时长（白花力气），关键任务延一秒总长延一秒。「该优化
 * 哪个任务」的答案是数学不是直觉。拓扑 DP（Kahn 入度消元）一遍出最长
 * 路径；环 fail-fast（DAG 契约）。
 *
 * <p>纯函数零状态、只算不长（排程归宿主）。
 */
public final class CriticalPathLength {

    private CriticalPathLength() {
    }

    /** 任务契约：id 非空白、duration ≥ 0（零时长任务合法——纯依赖占位）。 */
    public record Task(String id, long durationMillis) {

        public Task {
            if (id == null || id.isBlank() || durationMillis < 0) {
                throw new IllegalArgumentException(String.format(
                        "非法任务：id=%s, duration=%d", id, durationMillis));
            }
        }
    }

    /** 依赖边契约：from 先于 to；两端非空白。 */
    public record Dependency(String from, String to) {

        public Dependency {
            if (from == null || from.isBlank() || to == null || to.isBlank()) {
                throw new IllegalArgumentException(String.format(
                        "非法依赖：%s -> %s（两端须非空白）", from, to));
            }
        }
    }

    /**
     * @param criticalPathMillis 最长加权路径（总时长下界）
     * @param terminalTask       关键路径终点任务 id（空 DAG null）
     */
    public record Result(long criticalPathMillis, String terminalTask) {
    }

    /**
     * 关键路径入口。契约：边两端须在任务集（fail-fast）；图须无环
     *（Kahn 消元未尽即环——fail-fast 带剩余节点数）；null 任一按空。
     * 语义：EF[v] = dur[v] + max(EF[pred])；结果取 max EF。
     */
    public static Result longestPath(List<Task> tasks, List<Dependency> dependencies) {
        List<Task> taskWindow = tasks == null ? List.of() : tasks;
        List<Dependency> edgeWindow = dependencies == null ? List.of() : dependencies;
        Map<String, Long> duration = new HashMap<>();
        Map<String, List<String>> successors = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (Task t : taskWindow) {
            if (duration.putIfAbsent(t.id(), t.durationMillis()) != null) {
                throw new IllegalArgumentException("任务重复：" + t.id());
            }
            indegree.put(t.id(), 0);
        }
        for (Dependency d : edgeWindow) {
            if (!duration.containsKey(d.from()) || !duration.containsKey(d.to())) {
                throw new IllegalArgumentException(String.format(
                        "依赖端点不在任务集：%s -> %s", d.from(), d.to()));
            }
            successors.computeIfAbsent(d.from(), k -> new ArrayList<>()).add(d.to());
            indegree.merge(d.to(), 1, Integer::sum);
        }
        Map<String, Long> earliestFinish = new HashMap<>();
        Deque<String> ready = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                ready.add(e.getKey());
            }
        }
        long processed = 0;
        long best = 0;
        String terminal = null;
        while (!ready.isEmpty()) {
            String current = ready.poll();
            processed++;
            long finish = duration.get(current)
                    + maxPredFinish(current, earliestFinish, successors);
            earliestFinish.put(current, finish);
            if (finish > best || (finish == best && terminal == null)) {
                best = finish;
                terminal = current;
            }
            for (String next : successors.getOrDefault(current, List.of())) {
                if (indegree.merge(next, -1, Integer::sum) == 0) {
                    ready.add(next);
                }
            }
        }
        if (processed < taskWindow.size()) {
            throw new IllegalArgumentException(String.format(
                    "依赖图有环（%d 个任务不在拓扑序中）", taskWindow.size() - processed));
        }
        return new Result(best, terminal);
    }

    /** 前驱最大 EF：反向查——遍历边找 to==current（n·e 可换双邻接表，诚实边界：任务图小）。 */
    private static long maxPredFinish(String current, Map<String, Long> earliestFinish,
                                      Map<String, List<String>> successors) {
        long max = 0;
        for (Map.Entry<String, List<String>> e : successors.entrySet()) {
            if (e.getValue().contains(current)) {
                Long finish = earliestFinish.get(e.getKey());
                if (finish != null) {
                    max = Math.max(max, finish);
                }
            }
        }
        return max;
    }
}
