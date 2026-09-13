# effort #745 — 响应缓存权重预算 yml 装配（737 接线）

- 会话：G 会话 700 系第 46 轮 ｜ spec [745](../../../docs/spec/745-response-cache-weight-assembly.md) ｜ 票 [T1092](../tickets/T1092-response-cache-weight-assembly.md)/[T1093](../tickets/T1093-response-cache-weight-assembly-verify.md) ｜ impl646
- 借鉴：—（737 装配兑现，723 同模式）

## 勘察（排重）

- 737 权重预算是构造原语——ResilienceModule 的 ResponseCacheStore 创建点未传权重，yml 无法启用。

## 决定

`ResponseCache` record 扩第 4 组件 `maxWeightChars`（Long 默认 0=关；@ConstructorBinding 规范构造+3 参兼容构造——R39/R48 坑规避；负值 fail-fast）；ResilienceModule 装配点传 `responseCache().maxWeightChars()`+systemUTC；yml `buzhou.resilience.response-cache.max-weight-chars`+metadata。

## 测试

record 绑定+兼容构造默认关+负值拒绝。
