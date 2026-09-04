package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 健康三探针分层（spec 332 / T655，K8s probes 借鉴）：机制归类进
 * liveness（重启能治）/ readiness（摘流量能治——<b>缺省归类</b>，保守：
 * 外部依赖型故障最常见，重启治不了）/ startup（等待热身），三类独立裁决。
 *
 * <ul>
 *   <li>类内任一机制 DOWN → 该类 DOWN（failing 清单列出）；</li>
 *   <li>UNKNOWN 不连累任何裁决（未启用 ≠ 失能——312 同口径）；</li>
 *   <li>空类 → UP（无要求即满足）。</li>
 * </ul>
 *
 * <p>聚合面（/actuator/buzhou）答「哪个机制坏」；本类裁决面答「K8s 该
 * 做什么」——探针用错轻则无效重则恶性循环（外部依赖故障触发重启风暴）。
 */
public final class BuzhouProbes {

    /** 探针类（K8s 语义：liveness 失败→重启 / readiness 失败→摘流量 / startup 失败→等待）。 */
    public enum ProbeClass {
        LIVENESS, READINESS, STARTUP
    }

    /** 一类探针的裁决。 */
    public record Verdict(ProbeClass probeClass, BuzhouHealth.Status status,
            List<String> mechanisms, List<String> failing) {

        public Verdict {
            probeClass = java.util.Objects.requireNonNull(probeClass);
            status = status == null ? BuzhouHealth.Status.UP : status;
            mechanisms = List.copyOf(mechanisms == null ? List.of() : mechanisms);
            failing = List.copyOf(failing == null ? List.of() : failing);
        }
    }

    private final Map<ProbeClass, Set<String>> classification;

    /**
     * @param livenessMechanisms 点名为 liveness 的机制（缺省空——保守）
     * @param startupMechanisms  点名为 startup 的机制（缺省空）
     */
    public BuzhouProbes(Collection<String> livenessMechanisms,
            Collection<String> startupMechanisms) {
        Set<String> liveness = copyOf(livenessMechanisms);
        Set<String> startup = copyOf(startupMechanisms);
        for (String mechanism : liveness) {
            if (startup.contains(mechanism)) {
                throw new IllegalArgumentException("机制 " + mechanism
                        + " 同时点名为 liveness 与 startup——归类互斥（每机制恰一类）");
            }
        }
        Map<ProbeClass, Set<String>> map = new LinkedHashMap<>();
        map.put(ProbeClass.LIVENESS, liveness);
        map.put(ProbeClass.STARTUP, startup);
        this.classification = java.util.Collections.unmodifiableMap(map);
    }

    /** 启动期校验：点名机制必须存在（yml 错该红——312 同口径）。 */
    public void validateMechanisms(Collection<String> availableMechanisms) {
        Set<String> available = new LinkedHashSet<>(availableMechanisms);
        for (ProbeClass probeClass : new ProbeClass[]{ProbeClass.LIVENESS, ProbeClass.STARTUP}) {
            for (String mechanism : classification.get(probeClass)) {
                if (!available.contains(mechanism)) {
                    throw new IllegalArgumentException("探针归类（" + probeClass
                            + "）引用机制 " + mechanism + " 不存在；可用机制：" + available);
                }
            }
        }
    }

    /** 机制归类（未点名机制一律 READINESS——缺省保守）。 */
    public ProbeClass classOf(String mechanism) {
        if (classification.get(ProbeClass.LIVENESS).contains(mechanism)) {
            return ProbeClass.LIVENESS;
        }
        if (classification.get(ProbeClass.STARTUP).contains(mechanism)) {
            return ProbeClass.STARTUP;
        }
        return ProbeClass.READINESS;
    }

    /** 三类裁决（按机制状态快照）。 */
    public Map<ProbeClass, Verdict> verdicts(Map<String, BuzhouHealth.Status> statuses) {
        Map<ProbeClass, Set<String>> members = new LinkedHashMap<>();
        for (String mechanism : statuses.keySet()) {
            members.computeIfAbsent(classOf(mechanism), k -> new LinkedHashSet<>()).add(mechanism);
        }
        classification.forEach((probeClass, listed) ->
                listed.forEach(mechanism ->
                        members.computeIfAbsent(probeClass, k -> new LinkedHashSet<>())
                                .add(mechanism)));
        Map<ProbeClass, Verdict> result = new LinkedHashMap<>();
        for (ProbeClass probeClass : ProbeClass.values()) {
            List<String> mechanisms = List.copyOf(members.getOrDefault(probeClass, Set.of()));
            List<String> failing = mechanisms.stream()
                    .filter(m -> statuses.get(m) == BuzhouHealth.Status.DOWN)
                    .toList();
            result.put(probeClass, new Verdict(probeClass,
                    failing.isEmpty() ? BuzhouHealth.Status.UP : BuzhouHealth.Status.DOWN,
                    mechanisms, failing));
        }
        return result;
    }

    private static Set<String> copyOf(Collection<String> mechanisms) {
        Set<String> copy = new LinkedHashSet<>();
        if (mechanisms != null) {
            for (String mechanism : mechanisms) {
                if (mechanism == null || mechanism.isBlank()) {
                    throw new IllegalArgumentException("探针归类机制名非空");
                }
                copy.add(mechanism);
            }
        }
        return Set.copyOf(copy);
    }
}
