# 746 — dryRun×AlertGate 语义确认

> 来源：G 会话第 46 轮 = effort #746（spec 716 补验）/ [T1041](../../.wayfinder/tickets/T1041-dryrun-gate-shape.md) / [T1042](../../.wayfinder/tickets/T1042-dryrun-gate-verify.md) / impl 548。

## 背景

AlertGate（静默/抑制）只属实弹通知路径（spec 330）——dry-run 报告「通知前会被门裁决」的交互动需显式确认：dryRun 不经过 gate（纯推演）。

## 目标（测试域补验轮）

- gate 在场且会吞掉通知时：dryRun.wouldFire 仍如实报告（推演不受门抑制）；
- evaluate 实弹路径照常被 gate 吞（对照——门只属于实弹）；
- 语义入档：dry-run = 规则引擎推演，gate = 通知通道策略（正交）。

## 兼容性

纯测试域增量。
