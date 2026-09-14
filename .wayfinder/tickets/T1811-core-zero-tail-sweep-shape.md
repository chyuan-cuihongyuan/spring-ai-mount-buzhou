---
id: T1811
title: core 零覆盖尾巴清扫与判据收紧（AttachmentRenderer × CommandOutcome）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 4 轮：隔离 worktree 复扫（core 820 类）显示低覆盖档（<50% 且 miss≥10）已清空，仅剩 2 个零覆盖小类（AttachmentRenderer mis=6 / CommandBackend.CommandOutcome mis=4）。零覆盖判据 miss≥5 是否仍合理？尾巴如何收？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 4 轮 = effort #1203 / spec 1203 / impl 906）：

1. **判据收紧**：零覆盖定义从 `LINE_COVERED=0 && LINE_MISSED>=5` 收紧为 `LINE_MISSED>=1`（具名类型级）——R1 的 ≥5 门槛把 CommandOutcome（mis=4）这类「小而行为敏感」的类型藏在阈值之下（success() 是 run_command 判定谓词）。豁免口径不变：仅装配期匿名片段。
2. **AttachmentRenderer 补测面**（default 方法合同——**java.util 默认方法测试思想**：接口 default 体是接口合同的一部分，实现方覆写与否都需先有基线断言）：空 passthrough / maxChars≤0 不限 / 限内 passthrough / 超限截断至 maxChars（纯文本截断基线，覆写方按事实粒度截断是增强非替代）。
3. **CommandOutcome 补测面**：success() 谓词矩阵（exitCode=0 && !timedOut；exitCode≠0 → false；timedOut → false；truncated 不影响 success——截断是输出量控制非失败）+ record 分量透传。
4. **隔离 worktree 复扫纪律入档**：主工作区 core 复扫被并行会话构建竞争卡死（461 surefire XML 停滞 50 分钟，e84940f6 同源问题）——K 会话后续 core 证据一律走 `.scratch/k-wt` 隔离 worktree（本仓 e84940f6 记载的既定解法）。
5. **边界**：不改主代码；不追 record/enum 编译合成行（如 switch 桥接线）。
