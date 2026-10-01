package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * SCC 缩点 2-SAT（spec 10031 / X10063 / impl 2434）——Aspvall–Plass–Tarjan 1979
 * 思想（CLRS 思考题同源）：**子句 (a∨b) 引 ¬a→b、¬b→a 双蕴涵边成 2n 顶点蕴涵图，
 * SCC 缩点后正负字面量同分量判不可满足，否则按凝聚图拓扑序赋值**——布尔可满足
 * 的最简多项式子族线性直读面。SCC 判定消费 KosarajuScc（spec 10030）——库内
 * 组件互喂。
 *
 * <p>字面量口径：**1 起址有符号 int（DIMACS 惯例）**——+v 表变量 v 取真、−v 表
 * 取假（v∈[1,n]；0 起址的 −0≡0 无法表达否定，故不用）；子句为 int[2] 析取对。
 * 确定性契约：赋值由凝聚图 Kahn 序（同层分量编号升序）唯一决定，可复算圣像。
 * null 子句表/null 子句/非 int[2]/越界字面量（含 0）/非正变量数 fail-fast。
 */
public final class TwoSatSolver {

    private TwoSatSolver() {
    }

    /**
     * 求一组满足赋值。
     *
     * @param variableCount 变量数（变量域 1..n）
     * @param clauses 2-CNF 子句集（int[2] 析取对，1 起址有符号字面量）
     * @return 满足赋值（下标 i−1=变量 i 取值）；UNSAT 为 empty
     * @throws IllegalArgumentException null 子句表/null 子句/非 int[2]/越界字面量/非正变量数
     */
    public static Optional<boolean[]> solve(int variableCount, List<int[]> clauses) {
        if (variableCount < 1) {
            throw new IllegalArgumentException("变量数为正（实际 " + variableCount + "）");
        }
        if (clauses == null) {
            throw new IllegalArgumentException("子句表非 null");
        }
        int[][] implications = implicationEdges(variableCount, clauses);
        List<List<Integer>> components =
                KosarajuScc.components(2 * variableCount, Arrays.asList(implications));
        int[] componentOf = new int[2 * variableCount];
        for (int c = 0; c < components.size(); c++) {
            for (int literalVertex : components.get(c)) {
                componentOf[literalVertex] = c;
            }
        }
        for (int variable = 1; variable <= variableCount; variable++) {
            if (componentOf[node(variable)]
                    == componentOf[node(-variable)]) {
                return Optional.empty();
            }
        }
        int[] topoPosition = condensationTopoPosition(componentOf, implications);
        boolean[] assignment = new boolean[variableCount];
        for (int variable = 1; variable <= variableCount; variable++) {
            assignment[variable - 1] =
                    topoPosition[componentOf[node(variable)]]
                            > topoPosition[componentOf[node(-variable)]];
        }
        return Optional.of(assignment);
    }

    /** 蕴涵边表：(a∨b) ⇒ ¬a→b 与 ¬b→a（顶点域校验内联于此）。 */
    private static int[][] implicationEdges(int variableCount, List<int[]> clauses) {
        int[][] edges = new int[2 * clauses.size()][2];
        for (int i = 0; i < clauses.size(); i++) {
            int[] clause = clauses.get(i);
            if (clause == null || clause.length != 2) {
                throw new IllegalArgumentException("子句须为 int[2]（第 " + i + " 条）");
            }
            requireInUniverse(variableCount, clause[0], i);
            requireInUniverse(variableCount, clause[1], i);
            edges[2 * i] = new int[]{node(-clause[0]), node(clause[1])};
            edges[2 * i + 1] = new int[]{node(-clause[1]), node(clause[0])};
        }
        return edges;
    }

    private static void requireInUniverse(int variableCount, int literal, int clauseIndex) {
        if (literal == 0 || Math.abs(literal) > variableCount) {
            throw new IllegalArgumentException("字面量越界（第 " + clauseIndex
                    + " 条子句 " + literal + "，变量域 [1," + variableCount + "]）");
        }
    }

    /** 1 起址字面量 → 蕴涵图顶点：+v→2(v−1)，−v→2(v−1)+1（0 已被域校验拒绝）。 */
    private static int node(int literal) {
        return literal > 0 ? 2 * (literal - 1) : 2 * (-literal) - 1;
    }

    /**
     * 凝聚图 Kahn 拓扑位（跨分量边去重；同层分量编号升序出队——确定性口径）。
     */
    private static int[] condensationTopoPosition(int[] componentOf, int[][] implications) {
        int count = 0;
        for (int component : componentOf) {
            count = Math.max(count, component + 1);
        }
        TreeSet<Integer>[] successors = new TreeSet[count];
        TreeSet<Integer>[] predecessors = new TreeSet[count];
        for (int c = 0; c < count; c++) {
            successors[c] = new TreeSet<>();
            predecessors[c] = new TreeSet<>();
        }
        for (int[] edge : implications) {
            int from = componentOf[edge[0]];
            int to = componentOf[edge[1]];
            if (from != to) {
                successors[from].add(to);
                predecessors[to].add(from);
            }
        }
        int[] position = new int[count];
        boolean[] settled = new boolean[count];
        TreeSet<Integer> frontier = new TreeSet<>();
        for (int c = 0; c < count; c++) {
            if (predecessors[c].isEmpty()) {
                frontier.add(c);
            }
        }
        int next = 0;
        while (!frontier.isEmpty()) {
            int c = frontier.pollFirst();
            position[c] = next++;
            settled[c] = true;
            for (int successor : successors[c]) {
                predecessors[successor].remove(c);
                if (predecessors[successor].isEmpty() && !settled[successor]) {
                    frontier.add(successor);
                }
            }
        }
        return position;
    }
}
