package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * impl-756 / spec 1003：单键层级归属解析结果（spring config insights layer
 * attribution 借鉴——「这个生效值来自哪一层」一读即知）。
 *
 * @param dottedKey 查询的点分键
 * @param layer     供值层；三层皆无该键时为 {@code ABSENT}
 * @param value     生效值；{@code layer == ABSENT} 时恒为 {@code null}
 */
public record PolicyLayerAttribution(String dottedKey, Layer layer, Object value) {

    /**
     * 策略层。四层覆盖模型（spec 02）的前三层通用层；工具级走
     * {@link ToolPolicyMatcher}（匹配决策见 spec 1000），不入本枚举。
     */
    public enum Layer {
        /** 框架默认层（最低优先）。 */
        DEFAULTS,
        /** yml 全局层。 */
        YML,
        /** 绑定级（appId, agentName）层（前三层中最高优先）。 */
        BINDING,
        /** 三层皆无该键。 */
        ABSENT
    }

    public PolicyLayerAttribution {
        if (layer == Layer.ABSENT && value != null) {
            throw new IllegalArgumentException("ABSENT 归属下 value 必须为 null");
        }
        if (layer != Layer.ABSENT && value == null) {
            throw new IllegalArgumentException("非 ABSENT 归属必须携带生效值");
        }
    }
}
