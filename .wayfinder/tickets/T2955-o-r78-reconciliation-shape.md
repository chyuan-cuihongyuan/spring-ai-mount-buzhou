---
id: T2955
title: O 系 R78 对账轮的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-22
---

## Question)

第十三次周期核账的对账口径与撞车吸收怎么落？（spec 1877 / effort #1877 / R78）

## Resolution`

**快照补登 + 跨系对账修复 + 全仓 verify 绿**：①快照 regenerate
（QuorumConsistency 入档 1085→1091，共享 regenerate 一并吸收 Q 系
五类型）；②R73–R75 号段实撞吸收（远端先落为准、本系三轮改挂 R77
空闲位重排）；③Windows 环境确定性失败清零（租户沙箱分隔符 102 处
+ 全局静态顺序依赖 2 处 + 工具模块全局统计差分 1 处 + Unix-only
测试族 @EnabledOnOs 标注）；④Q 系 impl 号错位修复（R38–R41 四文件
整体 −1 错位，按其提交信息声明号纯改名 2037→2041）；⑤外域确定性
回归候选入档不代修（EvalItemTimeout 单跑两红——M-1500 系 09-15
波语义落库后首现，Q-R24 摇摆先例同款，显式排除有据）。
