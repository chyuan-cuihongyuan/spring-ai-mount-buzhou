package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 特性开关求值器（spec 2009 / T3119 / impl 1560）——OpenFeature 思想：
 * 求值永不抛出——未注册回 FLAG_NOT_FOUND 空值、targeting 谓词抛错回
 * ERROR + 默认变体兜底；每次求值带五态 reason（STATIC / TARGETING_
 * MATCH / DEFAULT / FLAG_NOT_FOUND / ERROR，OpenFeature errorCode 的
 * 工程并近似），reason 分布计数显形（求值质量对账——ERROR 占比高即
 * targeting 谓词有病灶）。
 *
 * <p>synchronized 小临界区；注册期一次、求值期只读。
 */
public final class FlagEvaluator {

    /** 求值结局五态（OpenFeature reason + errorCode 并近似）。 */
    public enum Reason {
        /** 无 targeting 声明的静态值。 */
        STATIC,
        /** targeting 谓词命中。 */
        TARGETING_MATCH,
        /** 谓词抛错的兜底默认（DEFAULT——OpenFeature 语义）。 */
        DEFAULT,
        /** flag 未注册（OpenFeature errorCode=FLAG_NOT_FOUND）。 */
        FLAG_NOT_FOUND,
        /** 求值器自身异常（OpenFeature errorCode=GENERAL）。 */
        ERROR
    }

    /** 求值结果：值 + reason + 变体名（未注册为 null 值）。 */
    public record FlagResolution(String variant, Reason reason) {
    }

    /** flag 定义：默认变体 + 可选 targeting 谓词与命中变体。 */
    public record FlagDefinition(
            String defaultVariant,
            Predicate<Map<String, String>> targeting,
            String targetedVariant) {

        public FlagDefinition {
            if (defaultVariant == null) {
                throw new IllegalArgumentException("defaultVariant 不能为 null");
            }
            if ((targeting == null) != (targetedVariant == null)) {
                throw new IllegalArgumentException("targeting 与 targetedVariant 须成对非空");
            }
        }

        /** 无 targeting 静态 flag 便捷构造。 */
        public static FlagDefinition staticFlag(String variant) {
            return new FlagDefinition(variant, null, null);
        }

        /** 带 targeting 的 flag 便捷构造（targeting/targetedVariant 同非空）。 */
        public static FlagDefinition targetedFlag(String defaultVariant,
                                                  Predicate<Map<String, String>> targeting,
                                                  String targetedVariant) {
            if (targeting == null || targetedVariant == null) {
                throw new IllegalArgumentException("targeting 与 targetedVariant 须成对非空");
            }
            return new FlagDefinition(defaultVariant, targeting, targetedVariant);
        }
    }

    private final Map<String, FlagDefinition> flags = new LinkedHashMap<>();
    private final Map<Reason, Long> reasonCounts = new EnumMap<>(Reason.class);

    /** 注册/覆盖 flag 定义（注册期一次约定——求值期不注册）。 */
    public synchronized void register(String flagName, FlagDefinition definition) {
        if (flagName == null || flagName.isBlank()) {
            throw new IllegalArgumentException("flagName 不能为空");
        }
        if (definition == null) {
            throw new IllegalArgumentException("definition 不能为 null");
        }
        flags.put(flagName, definition);
    }

    /**
     * 求值（永不抛出）：未注册 → FLAG_NOT_FOUND / null 值；targeting
     * 命中 → TARGETING_MATCH / 命中变体；未命中 → STATIC / 默认变体；
     * 谓词抛错 → DEFAULT / 默认变体兜底（计数 ERROR 与 DEFAULT 同记——
     * 兜底发生即求值质量事件）。
     */
    public synchronized FlagResolution evaluate(String flagName, Map<String, String> context) {
        if (flagName == null) {
            throw new IllegalArgumentException("flagName 不能为 null");
        }
        FlagDefinition definition = flags.get(flagName);
        if (definition == null) {
            count(Reason.FLAG_NOT_FOUND);
            return new FlagResolution(null, Reason.FLAG_NOT_FOUND);
        }
        Map<String, String> ctx = context == null ? Map.of() : context;
        if (definition.targeting() != null) {
            try {
                if (definition.targeting().test(ctx)) {
                    count(Reason.TARGETING_MATCH);
                    return new FlagResolution(definition.targetedVariant(), Reason.TARGETING_MATCH);
                }
                count(Reason.STATIC);
                return new FlagResolution(definition.defaultVariant(), Reason.STATIC);
            } catch (RuntimeException e) {
                count(Reason.DEFAULT); // 兜底发生
                count(Reason.ERROR);   // 病灶显形（分布对账用）
                return new FlagResolution(definition.defaultVariant(), Reason.DEFAULT);
            }
        }
        count(Reason.STATIC);
        return new FlagResolution(definition.defaultVariant(), Reason.STATIC);
    }

    /** reason 分布读数（求值质量对账面——ERROR/DEFAULT 占比即病灶率）。 */
    public synchronized Map<Reason, Long> reasonCounts() {
        return new EnumMap<>(reasonCounts);
    }

    /** 已注册 flag 名（只读快照）。 */
    public synchronized List<String> registeredFlags() {
        return List.copyOf(flags.keySet());
    }

    private void count(Reason reason) {
        reasonCounts.merge(reason, 1L, Long::sum);
    }
}
