# 717 — 工具调用图谱环检测

> 来源：G 会话第 18 轮 = effort #717（借鉴静态分析 call-graph cycle detection——DFS + rec-stack + 有界报告）/ [T985](../../.wayfinder/tickets/T985-graph-cycles-shape.md) / [T986](../../.wayfinder/tickets/T986-graph-cycles-verify.md) / impl 520。

## 背景

ToolGraphAnalyzer（spec 519）统计同轮相邻对有向边——模型循环调用（A→B→A 来回打转、自环 A→A 连续重调）在边计数上只是几条高计数边，环形态不可见。环 = 模型陷入重复模式的直接信号（工具循环断路器 spec 327 的上游观测依据）。

## 目标

- `ToolGraphAnalyzer.cycles(ToolGraphReport)` 纯函数：DFS 枚举初等环——
  - 每环以最小节点为锚（锚外不扩展——旋转去重：A→B→A 与 B→A→B 只报一次）；
  - 路径 visited 防自交；自环（A→A）单独识别；
  - `MAX_CYCLES=16` 有界封顶（超限停止搜索——枚举顺序确定→截断确定），输出按（长度, 字典序）稳定排序。
- 纯函数零 IO（报告即输入）；空图/无边图返回空表。

## 非目标

不做权重感知环分析（count 不参与环判定）；不做实时检测（离线分析面——实时拦截归 spec 327 循环断路器）。

## 测试

双节点环/自环检出、锚去重不双报、链/菱形零误报、>16 环封顶确定、空图空表。

## 兼容性

additive 纯函数；既有 analyze 语义零变化。
