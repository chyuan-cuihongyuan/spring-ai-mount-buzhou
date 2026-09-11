package io.github.chyuan_cuihongyuan.buzhou.core.hook;

/**
 * 回复流出站过滤器（spec 500 / T751）：模型回复离场前逐 chunk 改写——PII、
 * 秘密等出站纵深的挂点（BuzhouHook.replyStreamFilter() 提供实例，hook 序组合）。
 *
 * <p>生命周期契约：<b>每轮一次新建、单轮单用</b>（跨 chunk 有状态——跨界实体
 * 需要窗中缓冲）；同一轮内回调严格串行（流式 map 语义保证），实现无需线程安全。
 *
 * <p>组合语义：多个过滤器按 hook 序链接，{@code chunk → f1.filter → f2.filter →
 * out}；收口阶段 {@code f_i.flush()} 的产出依次过 {@code f_{i+1..n}.filter()}
 * 后拼接（core 负责编排，实现只管自身窗口）。
 */
public interface StreamTextFilter {

    /**
     * 处理一个 chunk，返回本 chunk 可离场的文本（可为空串——实体跨界待定，
     * 暂存内部窗口）。
     */
    String filter(String chunk);

    /** 轮次收口：排空内部缓冲，返回剩余文本（无残留返回空串）。 */
    String flush();
}
