---
id: T6067
title: R 会话 R34 语义化版本序的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

版本比较怎么机器性正确不因字典序误判？（spec 4033 /
effort #4033 / R34）

## Resolution

**SemVerOrder（core/policy）**：semver 2.0——parse 全量语法
fail-fast（禁前导零等）+ compare §11 优先级（pre 低于正式、
数字段数值比、数字 < 字母、build 不参与）；嵌套 Version
record 不另立面。
