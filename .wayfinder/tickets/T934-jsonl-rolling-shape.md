---
id: T934
title: 追加式 JSONL 轮转的形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-13
---

## Question

HealthTimelineJsonl / ShadowComparisonJsonl（长驻追加）与 PromptUsageJsonl（逐次追加）三类观测明细 JSONL 无限增长——磁盘撑爆是确定性风险。轮转怎么做？默认开还是 opt-in？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 43 轮 = effort #600 / spec 642 / impl 495）：`core.fs.RollingJsonlWriter`（Logback RollingFileAppender / logrotate 思想）：大小触发（当前文件 + 待写行 > maxBytes）→ 代际 shift（file→file.1，file.1→file.2 …，超 maxHistory 删最老）→ 重开追加。**默认开**（64MB × 3 代——资源保护是缺陷补全非行为变化；测试体量远低于阈值不破既有）；maxBytes/maxHistory ≤0 = 显式关（旧无界语义 escape hatch）。长驻两类换内部 writer；PromptUsage 增 4 参重载（append 前轮转检查）。轮转失败 best-effort 降级继续写原文件 + rotations()/rotationFailures() 计数（旁路语义不放大）。yml 细调键留装配扩散轮（非目标）。
