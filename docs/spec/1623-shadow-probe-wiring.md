# 1623 · 影子读探针接线（spec 189 孤类救活）

> 来源：N 会话 R24（effort #1623 / T2397–T2398 / impl 1176）。spec 1611 普查修复
> 第九弹：ShadowProbe（spec 189 / T561）建成即孤——注意与已接线的
> ShadowTrafficController（spec 49 流量对照）是两个机制。

## Solution

- 挂点：ResilienceAdvisor 主调用成功路径——`shadowMirrorIfSampled(request, response)`：
  确定性采样（sha256("mirror:"+modelName)%100 < rate）命中时，把同请求异步发首个
  备模型（effectiveFallbackModels 驱逐过滤后），主/影文本等值对照——
  agreed/diverged/errors 计数 + 最近 32 分歧样本（Istio mirror 思想：备模型的
  行为一致性 = 容量预案的信心面）。
- 执行器：复用每会话 deadlineExecutor（虚拟线程 submit 即忘）；会话关闭竞态
  REE 防护（影子本就是即忘旁路）。
- 配置：`buzhou.resilience.fallback.shadow-probe-percent`（0=关默认——Fallback
> 组扩参 + 5 参兼容构造保留）。

## Testing Decisions

- `ShadowProbeWiringTest` 四断言：一致/分歧双计数 + 分歧样本环；同 key 采样
  判定稳定；零采样率零执行；影子路故障计 error 不抛（旁路全吞）。
- 回归：resilience 全量 384 用例。

## Out of Scope

- 影子结果的健康联动（diverged 高时自动调整降级序——先让分歧可见）。
- 多备模型的轮换对照（当前恒对照首个——固定对照面语义清晰）。
