# 1408 — 断路器状态时长分析器

> 来源：L 会话第 9 轮 = effort #1408（票 T2117 / T2118 / impl 1061）。**换题记录**：原题「护栏判定混淆矩阵」勘察发现 JudgeCalibration（core/eval）已有同款 TP/TN/FP/FN+precision/recall/F1 数学——半撞换入 R37 题。借鉴：Resilience4j circuit breaker metrics（state duration 语义）。

## Problem Statement

`CircuitTransitionJournal`（spec 814 同型 core 侧）只记变迁时刻（from/to/atEpochMs 环 64）：「这台模型断路器这一小时有多久泡在 OPEN」须人工对时戳积段。crash-loop（spec 811 检测器之外）的量化画像——OPEN 占比/最长 OPEN 段——无分析面。

## 目标

- `CircuitStateDurationAnalyzer`（resilience/circuit，纯函数静态面，private 构造）：
  - `analyze(List<Transition>, nowEpochMs)` → `List<ModelDurations>`：按模型分组积段（[t_i→t_{i+1}) 归 transition_i 的 to 状态；末段延伸至 now）；
  - `ModelDurations(model, states, observedMillis, openMillis)` + 派生 `openShare()`（crash-loop 量化画像）；`StateDuration(state, totalMillis, segments, maxMillis)`；
  - states 按总时长降序（「主要泡在哪」第一眼可见）；无序输入内部升序后积分（journal recent 新→旧序容忍）；空输入空报告；
  - **采样型口径如实入档**：journal 有界环丢弃的旧变迁不参与积分——窗口以现存最早变迁为起点。
- 只读零接线：journal/断路器语义逐位不变。

## 兼容性

纯函数；Clock 以 nowEpochMs 入参注入（确定性测试）；不触 journal 内部状态（snapshot 读取方自治）。

## Out of Scope

- HALF_OPEN 探测成功率的联动判定（spec 836 半开探测读面已覆盖）。
- journal 容量外的全史积分（journal 无持久化——口径即采样窗）。
- 自动告警/降级联动（分析面不裁决）。
