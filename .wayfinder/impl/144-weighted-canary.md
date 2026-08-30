# 144 — 加权金丝雀降级链

**Parent:** spec 48 §B / [T175](../tickets/T175-weighted-canary.md)

**Status:** done

- [x] FallbackChain：canary-enabled 开关 + weights 配置态权重 + selectInitialTarget 会话稳定哈希加权选择（算法钉住）
- [x] ResilienceAdvisor：per-session 首选记忆 + canary.selected 事件每会话一次 + 金丝雀路径（目标熔断闸+单次 deadline）+ degradeFromCanary 链序回退（主模型在链首位、跳过已试）
- [x] ResilienceProperties.Fallback：canaryEnabled/weights 两键（2 参兼容构造保留）
- [x] 测试：1:9 大样本分布（±15pp 宽幅）+ 同会话粘住 / 失败回退主模型 + switched 事件 / 事件恰一次 / 默认关零变化
