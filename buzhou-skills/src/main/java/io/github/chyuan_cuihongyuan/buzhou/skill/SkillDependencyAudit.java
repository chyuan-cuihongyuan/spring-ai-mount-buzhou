package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 技能依赖图审计（spec 1843 / T2887 / impl 1444）——npm/pip 依赖解析
 * 思想：依赖图三病分诊——**环**（循环依赖，加载序无解）、**缺失依赖**
 *（边指向不存在的技能——声明了却没装的悬空引用）、**孤儿**（与任何边
 * 无关的声明技能——装了却没人依赖也不依赖人，目录噪音）。装得齐不等于
 * 依赖健康——三病各需各的处方（重构解环/补装/清理）。
 *
 * <p>纯函数零状态、只审计不解析（加载拓扑归宿主）；MAX_NODES 保险丝
 * 防长尾图失控。
 */
public final class SkillDependencyAudit {

    /** 图规模保险丝（节点数上限——超限 fail-fast 防失控遍历）。 */
    public static final int MAX_NODES = 10_000;

    private SkillDependencyAudit() {
    }

    /** 依赖边契约：两端非空白且不自指（自环在构造期拒绝）。 */
    public record Edge(String skill, String dependsOn) {

        public Edge {
            if (skill == null || skill.isBlank() || dependsOn == null
                    || dependsOn.isBlank()) {
                throw new IllegalArgumentException(String.format(
                        "非法依赖边：%s -> %s（两端须非空白）", skill, dependsOn));
            }
            if (skill.equals(dependsOn)) {
                throw new IllegalArgumentException("依赖边不能自指：" + skill);
            }
        }
    }

    /**
     * @param skills             声明技能总数
     * @param edges              依赖边总数
     * @param hasCycle           是否存在环
     * @param exampleCycle       首个发现的环路径（无环空表）
     * @param missingDependencies 缺失依赖边数（目标不在声明集）
     * @param orphanSkills       孤儿技能数（无边关联）
     */
    public record Audit(int skills, int edges, boolean hasCycle,
                        List<String> exampleCycle, long missingDependencies,
                        long orphanSkills) {
    }

    /**
     * 审计入口。契约：skillNames 规模 ≤ {@link #MAX_NODES}（fail-fast）；
     * null 边表按空表；边两端核契约。
     */
    public static Audit audit(Set<String> skillNames, List<Edge> edges) {
        Set<String> skills = skillNames == null ? Set.of() : skillNames;
        if (skills.size() > MAX_NODES) {
            throw new IllegalArgumentException(
                    "图规模超保险丝：" + skills.size() + " > " + MAX_NODES);
        }
        List<Edge> window = edges == null ? List.of() : edges;
        Map<String, List<String>> adjacency = new HashMap<>();
        Set<String> connected = new HashSet<>();
        long missing = 0;
        for (Edge e : window) {
            adjacency.computeIfAbsent(e.skill(), k -> new ArrayList<>())
                    .add(e.dependsOn());
            connected.add(e.skill());
            connected.add(e.dependsOn());
            if (!skills.contains(e.dependsOn())) {
                missing++;
            }
        }
        List<String> cycle = findCycle(skills, adjacency);
        long orphans = skills.stream().filter(s -> !connected.contains(s)).count();
        return new Audit(skills.size(), window.size(), !cycle.isEmpty(), cycle,
                missing, orphans);
    }

    /** 迭代 DFS 找环（返回首个环路径，无环空表——确定性入参序）。 */
    private static List<String> findCycle(Set<String> skills,
                                          Map<String, List<String>> adjacency) {
        Set<String> visited = new HashSet<>();
        for (String start : skills) {
            if (visited.contains(start)) {
                continue;
            }
            Deque<String> stack = new ArrayDeque<>();
            Set<String> onStack = new HashSet<>();
            Map<String, String> parent = new HashMap<>();
            stack.push(start);
            onStack.add(start);
            visited.add(start);
            // 显式栈 DFS（免深递归栈溢出）
            Deque<java.util.Iterator<String>> iters = new ArrayDeque<>();
            iters.push(adjacency.getOrDefault(start, List.of()).iterator());
            while (!stack.isEmpty()) {
                java.util.Iterator<String> it = iters.peek();
                if (it.hasNext()) {
                    String next = it.next();
                    if (!skills.contains(next)) {
                        continue; // 缺失依赖不入图（另有账）
                    }
                    if (onStack.contains(next)) {
                        // 回边——提取环路径
                        List<String> path = new ArrayList<>();
                        String cur = stack.peek();
                        path.add(next);
                        while (cur != null && !cur.equals(next)) {
                            path.add(cur);
                            cur = parent.get(cur);
                        }
                        path.add(next);
                        java.util.Collections.reverse(path);
                        return path;
                    }
                    if (!visited.contains(next)) {
                        parent.put(next, stack.peek());
                        stack.push(next);
                        onStack.add(next);
                        visited.add(next);
                        iters.push(adjacency.getOrDefault(next, List.of()).iterator());
                    }
                } else {
                    iters.pop();
                    onStack.remove(stack.pop());
                }
            }
        }
        return List.of();
    }
}
