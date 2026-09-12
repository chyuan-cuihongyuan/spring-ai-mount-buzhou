# 737 — ToolDenialLog 排序稳定性补验

> 来源：G 会话第 39 轮 = effort #737（spec 709 补验）/ [T1027](../../.wayfinder/tickets/T1027-denial-log-stability-shape.md) / [T1028](../../.wayfinder/tickets/T1028-denial-log-stability-verify.md) / impl 541。

## 背景

ToolDenialLog.topDenials 按 count 降序——同 count 并列时的稳定性与 entries 环形边界的幂等性需补验。

## 目标（测试域补验轮）

- 同 count 并列按 (role,tool) 字典序稳定；
- 同 (role,tool,reason) 重复记录计数累加；
- entries 最新在前且环满时窗口滑动稳定。

## 兼容性

纯测试域增量。
