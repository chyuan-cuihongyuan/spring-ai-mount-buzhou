# 1412 — 工具入参字节直方分桶读面

> 来源：L 会话第 13 轮 = effort #1412（票 T2125 / T2126 / impl 1065）。**换题记录**：原题 R49 摘要触发原因勘察发现 SummaryDegradeReasons（H 844）+CompactionRatioStats 已覆盖——换入 R41 题（MCP 参数直方的通用化：全部工具入参）。借鉴：Prometheus histogram（同 spec 1401 le 桶口径——请求/响应两侧对称计量，Datadog DogStatsD 对称 tag 思想）。

## Problem Statement

spec 1401 覆盖了工具**结果**字节分布；**入参**侧（模型发来的 arguments）全无读面：超长入参吃上下文预算、MCP 远端工具的参数体量、异常巨大的误构造调用——均不可见。

## 目标

- `ToolInputSizeHistogram implements BuzhouHook`（core/hook，opt-in 实例面，与 1401 结果侧同构对称）：
  - `beforeTool` 单点记账：`ctx.arguments()` 经 Jackson 序列化为 JSON 字节（UTF-8）落五幂次边界桶（256/1K/4K/16K/64K 同边界）+ 溢出桶；
  - `executed`（入口总量）+ `totalBytes`（精确累计，均值=totalBytes/executed）；
  - 守恒式：`executed = b0+…+b4+overflow`；
  - 嵌套 `record Snapshot(b0..overflow, executed, totalBytes)` + `stats()` + `resetForTest()`。
- **成本口径如实入档**：hook 内自行序列化 arguments（与 HookedToolCallback 的 serializeArguments 各一次）——仅注册本 hook 的宿主承担，未注册零开销。

## 兼容性

纯 opt-in 读面：beforeTool 返回 CONTINUE 对链零影响；入参改写（replaceArguments）发生在链后——测量点为链前原始入参（口径显式）。

## Out of Scope

- 按工具名 tag（基数红线，同 1401）。
- 结果侧（1401 已覆盖，对称不重复）。
- MCP 单独分面（全部工具统一入桶；MCP 工具名维度留进程内聚合模式另轮）。
