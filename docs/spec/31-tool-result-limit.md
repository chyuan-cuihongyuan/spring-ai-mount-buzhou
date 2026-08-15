# Spec 31 — 工具结果尺寸防护（ToolResultLimiter）

> effort #6（T110 / impl-85）。MCP server 万行查询 / 远程 fetch 整页 HTML 场景的
> 上下文护栏；Anthropic「tool_result 是上下文膨胀最大源」的工程对策。

## Problem Statement

工具结果无尺寸上限即入模型上下文：本地工具侧有 spill/ReadRange 兜底，MCP 侧裸奔——
一次万行查询可吃掉整个上下文窗口（后续轮次全失效）或直接触发 provider 上下文溢出。

## Solution

`HarnessToolCallingManager` 结果统一出口字符限幅：默认 20K 字符，超限截断（保留头部）
+ 结构化提示尾（模型可感知原始尺寸并细化查询/分页重读）；per-tool glob 覆盖
（含 -1 豁免）；默认豁免 `read_range`（spill 自治理）。指标
`buzhou.tools.result-truncated`（tag tool=工具名，有限集口径）。

## User Stories

1. As a 应用开发者, I want MCP 大结果不炸上下文, so that 一次粗暴查询不废掉整个会话。
2. As a 模型, I want 截断带原始尺寸提示, so that 我知道被截了多少并改用分页。
3. As a 平台运维, I want per-tool 覆盖与豁免, so that 自治理工具（read_range）不受限。
4. As a SRE, I want 截断有指标, so that 频繁截断的工具可定位并优化。

## Implementation Decisions

- **防护位置**：`executeToolCalls` 响应收集点（全部工具统一出口，本地/MCP 通吃）；
  per-session 可经 `SessionAssemblyContext.toolManager().setResultLimiter(...)` 覆盖。
- **阈值口径**：字符数（默认 20_000 ≈ 5K token；token 估算需全量扫描，字符数零歧义）。
- **超限行为**：保留头部 + `…[结果已截断：原始 N 字符，超出上限 M。请细化查询或分页读取所需部分]`；
  **不自动转 spill**（自动 spill 需结果-调用关联落盘，复杂度不抵收益，fog）。
- **配置**：`buzhou.tools.result-limit-chars`（-1 不限）+ `buzhou.tools.result-limit-overrides`
  （glob 键 → 字符数/-1；追加式叠加默认 read_range 豁免，同键改值）。
- **全局默认**：`ToolResultLimiterHolder`（BuzhouMetricsHolder 同型 Holder）——auto-config
  启动期据配置设定，非 Spring 编程式用户获得同等默认。
- **事件降级**：截断信号 = 结果内提示尾（模型与观测双可见）+ 指标；不发 SessionEvent
  （manager 无事件通道，且结果自含信号已够观测）。

## Testing Decisions

- 单测（ToolResponse 直接驱动）：截断+提示尾 / 限内同实例直传 / glob 豁免与覆盖
  （read_range 默认豁免、前缀通配、同键改值）/ 禁用档 / glob 匹配语义五用例。
- 既有全量回归（289 用例）验证零行为破坏（限内结果同实例返回）。

## Out of Scope

- 自动 spill 化截断结果（fog）。
- 结构化（非字符串）工具结果的分块防护（当前 responseData 均字符串口径）。
- per-server 粒度（需 server→tool 映射维护，等 T86 基线消费成熟再议）。

## Further Notes

- 与 spec 02（spill）/18（MCP 漂移）正交：本地大结果走 spill 自治理，本 spec 护的是
  「不自治理的工具」（主要是 MCP 与业务自定义工具）。
