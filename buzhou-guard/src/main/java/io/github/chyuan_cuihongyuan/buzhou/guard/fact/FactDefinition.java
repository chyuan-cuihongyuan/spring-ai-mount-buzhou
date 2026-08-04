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

    /** 判定器：本次工具调用是否产生事实；命中返回事实载荷名 + value。 */
    Optional<Fact> judge(ToolCallContext ctx);

    /** 渲染器：事实 → 注入 prompt 文本。 */
    String render(Fact fact);

    /** 存活轮次：1=一次性，大值=累积（spec ttl 语义）。 */
    default int ttl() {
        return 1;
    }
}
