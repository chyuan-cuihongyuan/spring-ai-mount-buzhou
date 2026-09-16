package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调度松弛量（spec 1873 / T2947 / impl 1474）——项目管理 CPM 的松弛量
 *（float/slack）：任务**最晚可延迟多久**不拖总工期——关键路径上任务
 * float=0（动一毫秒总长延一毫秒），非关键任务有缓冲（float>0 可让路）。
 * 「谁能让路、让多久」是并行编排削峰的依据：float 大的任务延后执行，
 * 关键任务独占资源。
 *
 * <p>与 {@link CriticalPathLength} 配对：那是总下界与关键终点，这是
 * 逐任务缓冲量。正向（最早开始/完成）+ 反向（最晚开始/完成）双 DP。
 *
 * <p>纯函数零状态；环 fail-fast。
 */
public final class ScheduleFloat {

    private ScheduleFloat() {
    }

    /** 单任务松弛契约。 */
    public record Task(String id, long durationMillis) {

        public Task {
            if (id == null || id.isBlank() || durationMillis < 0) {
                throw new IllegalArgumentException(String.format(
                        "非法任务：id=%s, duration=%d", id, durationMillis));
            }
        }
    }

    /** 依赖边契约：from 先于 to。 */
    public record Dependency(String from, String to) {

        public Dependency {
            if (from == null || from.isBlank() || to == null || to.isBlank()) {
                throw new IllegalArgumentException(String.format(
                        "非法依赖：%s -> %s", from, to));
            }
        }
    }

    /**
     * @param floatMillis 松弛量（最晚可延迟；关键任务 0）
     * @param earliestStartMillis 最早开始（前驱 EF 最大值）
     */
    public record TaskFloat(String id, long floatMillis, long earliestStartMillis) {
    }

    /**
     * 全任务松弛量。语义：总工期 T = 最长路径；LS[v] = T −（v 到汇的最长
     * 距）− dur[v]；float[v] = LS[v] − ES[v]。契约：边端点在任务集、
     * 无环、无重复（fail-fast）；null 任一按空。
     */
    public static Map<String, TaskFloat> floats(List<Task> tasks,
                                                List<Dependency> dependencies) {
        List<Task> taskWindow = tasks == null ? List.of() : tasks;
        List<Dependency> edgeWindow = dependencies == null ? List.of() : dependencies;
        Map<String, Long> duration = new HashMap<>();
        Map<String, List<String>> successors = new HashMap<>();
        Map<String, List<String>> predecessors = new HashMap<>();
        for (Task t : taskWindow) {
            if (duration.putIfAbsent(t.id(), t.durationMillis()) != null) {
                throw new IllegalArgumentException("任务重复：" + t.id());
            }
        }
        for (Dependency d : edgeWindow) {
            if (!duration.containsKey(d.from()) || !duration.containsKey(d.to())) {
                throw new IllegalArgumentException(String.format(
                        "依赖端点不在任务集：%s -> %s", d.from(), d.to()));
            }
            successors.computeIfAbsent(d.from(), k -> new ArrayList<>()).add(d.to());
            predecessors.computeIfAbsent(d.to(), k -> new ArrayList<>()).add(d.from());
        }
        // 正向：ES/EF（拓扑序由递归备忘实现——任务图小，诚实边界同 CPM 轮）
        Map<String, Long> earliestStart = new HashMap<>();
        for (String id : duration.keySet()) {
            earliestStart(id, duration, predecessors, earliestStart, new java.util.HashSet<>());
        }
        long total = 0;
        for (String id : duration.keySet()) {
            total = Math.max(total, earliestStart.get(id) + duration.get(id));
        }
        // 反向：到汇最长距（决定 LS）
        Map<String, Long> toSink = new HashMap<>();
        for (String id : duration.keySet()) {
            longestToSink(id, duration, successors, toSink, new java.util.HashSet<>());
        }
        Map<String, TaskFloat> result = new HashMap<>();
        for (Map.Entry<String, Long> e : duration.entrySet()) {
            String id = e.getKey();
            long es = earliestStart.get(id);
            long ls = total - toSink.get(id) - e.getValue();
            result.put(id, new TaskFloat(id, ls - es, es));
        }
        return Map.copyOf(result);
    }

    private static long earliestStart(String id, Map<String, Long> duration,
                                      Map<String, List<String>> predecessors,
                                      Map<String, Long> memo, java.util.Set<String> visiting) {
        Long cached = memo.get(id);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(id)) {
            throw new IllegalArgumentException("依赖图有环（经 " + id + "）");
        }
        long max = 0;
        for (String pred : predecessors.getOrDefault(id, List.of())) {
            long predFinish = earliestStart(pred, duration, predecessors, memo, visiting)
                    + duration.get(pred);
            max = Math.max(max, predFinish);
        }
        visiting.remove(id);
        memo.put(id, max);
        return max;
    }

    private static long longestToSink(String id, Map<String, Long> duration,
                                      Map<String, List<String>> successors,
                                      Map<String, Long> memo, java.util.Set<String> visiting) {
        Long cached = memo.get(id);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(id)) {
            throw new IllegalArgumentException("依赖图有环（经 " + id + "）");
        }
        long max = 0;
        for (String next : successors.getOrDefault(id, List.of())) {
            max = Math.max(max, duration.get(next)
                    + longestToSink(next, duration, successors, memo, visiting));
        }
        visiting.remove(id);
        memo.put(id, max);
        return max;
    }
}
