---
id: T1455
title: Hook 链解析顺序快照读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 3 轮：Hook 链解析顺序快照（Kong plugin priority）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 3 轮 = effort #1002 / spec 1002 / impl 755）：缺口成立——HookChain 构造期按 order 升序、同序按名稳定排序与 disabled 过滤已是事实标准（Kong plugin priority 思想已内嵌），但两个诊断盲区：①disabled 配置里拼错的 hook 名**静默蒸发**（幽灵禁用零信号）；②解析后派发序不带 order 值可见性（配置排查要翻源码）。落点 core.hook：新公共 record `ChainComposition(resolvedHookNames 派发序, ghostDisabledNames 幽灵禁用集)` + `HookChain.composition()` 只读快照（构造期一次计算、字段 final）。排序语义与幽灵检测零行为变化——纯显形。
