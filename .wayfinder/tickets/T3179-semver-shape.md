---
id: T3179
title: 版本要求判定的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

技能运行时版本要求的兼容范围怎么表达判定？（spec 2039 / effort #2039 / R40）

## Resolution

**npm semver range 不可变判定器 `VersionRequirement`（buzhou-skills）**：
parse 六算子（^ CARET 同主兼容含 0.x 锁定特例（^0.2.3 锁次/^0.0.3 锁
补丁——semver 惯例）、~ TILDE 次锁、>=/>、精确、*）+satisfies 点分
逐段短补 0（"1.10">"1.9" 正确）+prerelease 低于同基段（与
SkillChannelResolver 同口径）。
