package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hook 顺序碰撞审计（spec 1422 / T2145 / impl 1075）——Spring ordered-bean
 * 审计思想：同 order 的钩子由 ChainComposition 以<b>名字典序</b>兜底排序——
 * 分发确定，但保序靠「名字恰好」是装配巧合：重命名一个 Hook 即改变链序
 * （beforeTool 的裁决先后语义随名字漂移）。本审计把同序碰撞组显形，
 * 供宿主在装配期巡查脆性（修复 = 给钩子分配显式错开的 order）。
 *
 * <p>纯函数零状态：不触 HookChain 运行期；只读不裁决。只列碰撞组
 * （≥2 个钩子同序）——唯一 order 不占报告。
 */
public final class HookOrderAudit {

    private HookOrderAudit() {
    }

    /**
     * @param order     碰撞的 order 值
     * @param hookNames 该 order 上的钩子名（字典序）
     */
    public record OrderGroup(int order, List<String> hookNames) {
    }

    /**
     * @param hookCount      审计的钩子总数
     * @param collisions     碰撞组（order 升序）
     * @param collisionCount 碰撞组数（= collisions.size()，0 = 无脆性）
     */
    public record Report(int hookCount, List<OrderGroup> collisions, int collisionCount) {
    }

    /** 审计入口：宿主装配的钩子清单（顺序无关）。 */
    public static Report analyze(List<BuzhouHook> hooks) {
        if (hooks == null || hooks.isEmpty()) {
            return new Report(0, List.of(), 0);
        }
        Map<Integer, List<String>> byOrder = new LinkedHashMap<>();
        for (BuzhouHook hook : hooks) {
            String name = hook.name();
            byOrder.computeIfAbsent(hook.order(), k -> new ArrayList<>()).add(name);
        }
        List<OrderGroup> collisions = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> e : byOrder.entrySet()) {
            if (e.getValue().size() >= 2) {
                List<String> names = e.getValue().stream().distinct().sorted().toList();
                collisions.add(new OrderGroup(e.getKey(), names));
            }
        }
        collisions.sort(Comparator.comparingInt(OrderGroup::order));
        return new Report(hooks.size(), List.copyOf(collisions), collisions.size());
    }
}
