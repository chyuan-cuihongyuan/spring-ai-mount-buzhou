# 1401 — 工具结果字节直方分桶读面

> 来源：L 会话第 2 轮 = effort #1401（票 T2103 / T2104 / impl 1054）。借鉴：Prometheus histogram（le 幂次边界桶 + 累计计数——分布形状从桶占比直读，不需要存原始样本）。

## Problem Statement

工具执行产物（回喂模型的文本）的字节分布不可见：`buzhou.tool.duration` timer 只有时延维（spec 108/700/1001 族），错误签名只聚类异常族（spec 83）。宿主无法回答「工具结果普遍是玩具级短串还是占满上下文预算的大块头」——回喂预算压力、spill 离载（offload）收益评估均无数据。

## 目标

- `ToolResultSizeHistogram implements BuzhouHook`（core/hook，opt-in 实例面）：`afterTool` 单点记账，返回 CONTINUE 对链零影响。
  - 五幂次边界桶：`<256B / <1KB / <4KB / <16KB / <64KB` + `≥64KB` 溢出桶（`BOUNDARIES = {256, 1024, 4096, 16384, 65536}`）；
  - `executed`（入口总量）/ `failed`（error 路径，错误反馈文案不入字节分布）/ `totalBytes`（成功产物 UTF-8 字节精确累计，均值=totalBytes/successes）；
  - 守恒式：`successes = b0+…+b4+overflow`、`executed = successes + failed`；
  - 字节口径与 J 会话 ReadFileStats/WriteFileStats（UTF-8）一致，轴间可比。
- 嵌套 `record Snapshot(...)`（含 `successes()` 派生）+ `stats()` + `resetForTest()`。

## 兼容性

纯 opt-in 读面：未注册零开销（无 Hook 实例）；注册后只读不裁决（CONTINUE），工具执行/错误反馈/慢榜/timer 语义逐位不变。测量点为 hook 链改写前的原始执行产物（afterTool Replace 改写后模型所见不入本分布——口径显式）。

## Out of Scope

- 按工具名 tag 分桶（基数失控——MetricNamingGuard 红线；per-tool 面归 ToolTimingAggregator 同款进程内聚合模式，留后续）。
- 请求侧（toolInput）字节分布（另轴）。
- 无界精确分位（桶近似即满足分布形状诉求；t-digest 级精度留后续）。
