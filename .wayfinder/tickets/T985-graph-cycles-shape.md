---
id: T985
title: 工具调用图谱环检测的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

ToolGraphAnalyzer（spec 519）已有同轮相邻对有向边统计——模型循环调用（A→B→A 来回打转）在边计数上不可见。环检测怎么落（初等环枚举的组合爆炸怎么控）？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 18 轮 = effort #717 / spec 717 / impl 520）：ToolGraphAnalyzer 增纯函数 `cycles(ToolGraphReport)`——DFS 枚举初等环：每环以**最小节点为锚**（锚外不扩展——旋转去重惯例），路径 visited 集合防自交；`MAX_CYCLES=16` 有界封顶（超限停止搜索——基数有界纪律；枚举顺序确定性→截断确定），输出按（长度,字典序）稳定排序。自环（A→A）单独识别（同轮连续两次同工具）。纯函数零 IO——报告即输入。借鉴静态分析 call-graph cycle detection（DFS + rec-stack + 有界报告）。
