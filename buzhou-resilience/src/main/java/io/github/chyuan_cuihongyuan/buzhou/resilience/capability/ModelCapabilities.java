package io.github.chyuan_cuihongyuan.buzhou.resilience.capability;

/**
 * 单模型能力声明（spec 502 / T755，LiteLLM Router model capabilities 借鉴）。
 *
 * @param vision        支持多模态图像输入（media 请求可路由）
 * @param tools         支持 function calling（带工具请求可路由）
 * @param contextWindow 上下文窗口大小（token）；未知 -1（当前仅声明面，
 *                      窗口预算归 memory 动态预算域）
 */
public record ModelCapabilities(boolean vision, boolean tools, int contextWindow) {

    /** 未知窗口哨兵。 */
    public static final int CONTEXT_WINDOW_UNKNOWN = -1;

    public ModelCapabilities {
        if (contextWindow == 0 || contextWindow < CONTEXT_WINDOW_UNKNOWN) {
            throw new IllegalArgumentException(
                    "contextWindow 须为正或 " + CONTEXT_WINDOW_UNKNOWN + "（未知；当前 " + contextWindow + "）");
        }
    }

    /** 全能力 + 未知窗口的便捷工厂（yml 简写场景）。 */
    public static ModelCapabilities full() {
        return new ModelCapabilities(true, true, CONTEXT_WINDOW_UNKNOWN);
    }
}
