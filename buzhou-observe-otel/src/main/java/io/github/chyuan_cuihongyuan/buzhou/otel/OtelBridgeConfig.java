package io.github.chyuan_cuihongyuan.buzhou.otel;

/**
 * OTel 导出桥配置（spec 03 配置项表，前缀 {@code buzhou.observability.otel.*}）。
 *
 * <p>本模块缺省关闭（{@code otel.enabled=false}）：模块引入后仍需显式开启才生效；
 * 关闭时装配侧不产出 {@link io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink}，
 * 主链路零开销（不进 {@code enqueue} 旁路）。
 *
 * @param enabled        OTel 导出桥总开关（默认 false）
 * @param includeContent 是否在导出的 Event 属性里携带思维链/回复正文/工具入参出参原文（默认 false，
 *                       对齐官方 {@code include-content} 默认关的隐私立场；spec 03 推演 #15 将此口径
 *                       由「仅 THINKING/FINAL_REPLY content」泛化到全部内容型 payload 字段）
 */
public record OtelBridgeConfig(boolean enabled, boolean includeContent) {

    /** 缺省：关闭。 */
    public static OtelBridgeConfig defaults() {
        return new OtelBridgeConfig(false, false);
    }

    /** 开启导出桥，正文仍按默认不携带。 */
    public static OtelBridgeConfig enabledDefaults() {
        return new OtelBridgeConfig(true, false);
    }
}
