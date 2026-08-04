package io.github.chyuan_cuihongyuan.buzhou.guard.fact;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;

import java.util.Optional;

/**
 * 事实采集器三要素脚手架（spec 07 FactCollector 三要素）。
 *
 * <p>业务实现并注册为 Bean，框架（{@code FactCollectorHook}, afterTool, order 200）管存储、注入、过期。
 * 判定器应从**入参**判定语义而非硬匹配工具名（蓝本例：带 {@code tableId} 的 {@code upsertTable}
 * 才是改表，新建表不触发）。
 */
public interface FactDefinition {

    /** producer 名（入 fact.{producer}.{name} key 命名空间）。 */
    String name();

    /**
     * 判定器：本次工具调用是否产生事实。
     *
     * <p><b>key 契约</b>：返回的 {@link Fact#key()} 只承载 name 段（如 {@code "table-1"}），
     * 不含 {@code fact.{producer}.} 前缀——FactCollectorHook 会用 {@link #name()} 自动拼接完整
     * 命名空间 key；返回已命名空间化的 key 会导致双重前缀。返回 Fact 的 producer/createdTurn/ttl
     * 字段被忽略（hook 以本 definition 的 name/ttl 与当前轮次覆盖）。
     */
    Optional<Fact> judge(ToolCallContext ctx);

    /** 渲染器：事实 → 注入 prompt 文本。 */
    String render(Fact fact);

    /** 存活轮次：1=一次性，大值=累积（spec ttl 语义）。 */
    default int ttl() {
        return 1;
    }
}
