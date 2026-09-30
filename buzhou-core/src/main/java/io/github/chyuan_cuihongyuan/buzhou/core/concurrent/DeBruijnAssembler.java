package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;

/**
 * 德布鲁因图组装（spec 10021 / X10043 / impl 2424）——de Bruijn 图
 * 欧拉路径思想（「k-mer 边化+欧拉路径重构」——SPAdes/Velvet 基因
 * 组组装器同源）：**读段切 k-mer、(k−1)-mer 为顶点 k-mer 为有向
 * 边，欧拉路径判定（起讫度差 +1/−1 其余持平）+Hierholzer 逐环拼
 * 接重构序列**——重叠排序 O(n²) 全比对的病解互补面。欧拉条件破
 * 坏（分支/断链）fail-fast；非 ACGT/读段过短/k 越域 fail-fast；
 * 邻接 TreeMap 确定序（同构同解）。
 */
public final class DeBruijnAssembler {

    private DeBruijnAssembler() {
    }

    /**
     * 组装（reads 无序集——图语义与输入序无关）。
     *
     * @throws IllegalArgumentException null/空/字母域/k 越域/读段过短/非欧拉
     */
    public static String assemble(List<String> reads, int k) {
        if (reads == null || reads.isEmpty()) {
            throw new IllegalArgumentException("读段集非空非 null");
        }
        if (k < 2) {
            throw new IllegalArgumentException("k≥2（实际 " + k + "）");
        }
        for (String read : reads) {
            if (read == null || read.length() < k) {
                throw new IllegalArgumentException("读段长度 ≥k（实际 "
                        + (read == null ? "null" : read.length()) + " vs " + k + "）");
            }
            for (int i = 0; i < read.length(); i++) {
                char base = read.charAt(i);
                if (base != 'A' && base != 'C' && base != 'G' && base != 'T') {
                    throw new IllegalArgumentException("字母域 {A,C,G,T}（实际 " + base + "）");
                }
            }
        }
        Map<String, TreeMap<String, Integer>> graph = new TreeMap<>();
        Map<String, Integer> outDegree = new TreeMap<>();
        Map<String, Integer> inDegree = new TreeMap<>();
        for (String read : reads) {
            for (int i = 0; i + k <= read.length(); i++) {
                String from = read.substring(i, i + k - 1);
                String to = read.substring(i + 1, i + k);
                graph.computeIfAbsent(from, x -> new TreeMap<>())
                        .merge(to, 1, Integer::sum);
                outDegree.merge(from, 1, Integer::sum);
                inDegree.merge(to, 1, Integer::sum);
            }
        }
        String start = eulerianStart(graph, outDegree, inDegree);
        return hierholzer(graph, start, totalEdges(outDegree));
    }

    /** 欧拉起点判定（度差 +1 至多一个/−1 至多一个/其余持平）。 */
    private static String eulerianStart(Map<String, TreeMap<String, Integer>> graph,
                                        Map<String, Integer> outDegree, Map<String, Integer> inDegree) {
        Set<String> vertices = new LinkedHashSet<>();
        vertices.addAll(graph.keySet());
        vertices.addAll(inDegree.keySet());
        String start = null;
        int extraOut = 0;
        int extraIn = 0;
        for (String v : vertices) {
            int out = outDegree.getOrDefault(v, 0);
            int in = inDegree.getOrDefault(v, 0);
            if (out - in == 1) {
                extraOut++;
                start = v;
            } else if (in - out == 1) {
                extraIn++;
            } else if (in != out) {
                throw new IllegalArgumentException("读段集须可线性重构（顶点 " + v + " 度差 " + (out - in) + "）");
            }
        }
        int excess = extraOut + extraIn;
        if (excess != 0 && excess != 2) {
            throw new IllegalArgumentException("读段集须可线性重构（度差顶点数 " + excess + "）");
        }
        if (start == null) {
            start = vertices.iterator().next();
        }
        return start;
    }

    /** Hierholzer 标准栈式欧拉路径（弹栈序逆置=顶点路径）。 */
    private static String hierholzer(Map<String, TreeMap<String, Integer>> graph,
                                     String start, int totalEdges) {
        Deque<String> stack = new ArrayDeque<>();
        List<String> vertexPath = new ArrayList<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String v = stack.peek();
            TreeMap<String, Integer> neighbors = graph.get(v);
            String next = null;
            if (neighbors != null) {
                for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                    if (entry.getValue() > 0) {
                        next = entry.getKey();
                        break;
                    }
                }
            }
            if (next != null) {
                neighbors.merge(next, -1, Integer::sum);
                stack.push(next);
            } else {
                vertexPath.add(stack.pop());
            }
        }
        Collections.reverse(vertexPath);
        if (vertexPath.size() - 1 != totalEdges) {
            throw new IllegalArgumentException("读段集须可线性重构（图非连通欧拉——实际路径覆盖 "
                    + (vertexPath.size() - 1) + " 边 / 全图 " + totalEdges + " 边）");
        }
        StringBuilder assembled = new StringBuilder(vertexPath.get(0));
        for (int i = 1; i < vertexPath.size(); i++) {
            assembled.append(vertexPath.get(i).charAt(vertexPath.get(i).length() - 1));
        }
        return assembled.toString();
    }

    private static int totalEdges(Map<String, Integer> outDegree) {
        int total = 0;
        for (int count : outDegree.values()) {
            total += count;
        }
        return total;
    }
}
