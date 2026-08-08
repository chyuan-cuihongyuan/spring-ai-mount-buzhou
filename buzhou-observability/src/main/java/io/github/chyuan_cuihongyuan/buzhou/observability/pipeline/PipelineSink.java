package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

/**
 * 管线旁路观察者：在 {@link BaseSpanRecorder#enqueue} 时刻同步收到每一份落库前的
 * {@link SpanRecord}/{@link EventRecord}（按调用方入队顺序，保留 open→event→close 时序）。
 *
 * <p>用于 OTel 导出桥（{@code buzhou-observe-otel}）等旁路消费者：在 Span/Event 落库的同时
 * 把认知模型映射为 OTel span 导出（spec 03「OTel 导出桥」）。
 *
 * <p><b>实现约束</b>：
 * <ul>
 *   <li>回调在采集线程（advisor / 工具回调虚拟线程）同步执行，必须轻量、不得抛异常——
 *       {@link BaseSpanRecorder} 会兜底吞掉 {@link RuntimeException}，但抛异常会污染主链路日志；</li>
 *   <li>并发可见：同一会话的并发工具调用会在不同线程同时回调，实现方须自行保证内部状态线程安全；</li>
 *   <li>仅观察 Span/Event；注入快照（{@code InjectionSnapshot}）不经过本接口。</li>
 * </ul>
 *
 * <p>本接口是 observe-otel / observe-dashboard 可选模块（09 模块工程档白名单的二层边）消费的
 * <b>受支持扩展点</b>——非 {@code internal}（跨模块可引用），随 {@code buzhou-observability} 同版本演进
 * （由 {@code buzhou-bom} 统一版本，无独立语义版本承诺）。
 */
public interface PipelineSink {

    /** Span 记录入队（RUNNING 开启态 + 终态关闭态均会回调，同 spanId）。 */
    void onSpan(SpanRecord record);

    /** Event 记录入队。{@code record.spanId()} 指向归属 Span。 */
    void onEvent(EventRecord record);
}
