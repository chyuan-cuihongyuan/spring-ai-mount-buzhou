# 632 — 停机排水取消原因 E2E

> 来源：F 会话第 33 轮 = effort #600（spec 606 接线的真路径补验）/ [T914](../../.wayfinder/tickets/T914-drain-cause-e2e-shape.md) / [T915](../../.wayfinder/tickets/T915-drain-cause-e2e-verify.md) / impl 485。

## 背景

SHUTDOWN_DRAIN 接线（606）此前仅签名兼容级覆盖——事件 payload 真带 cause 无钉住。

## 目标

E2E：挂死在途 Turn + 优雅停机 → session.cancelled {AFTER_CURRENT_TURN, SHUTDOWN_DRAIN}。

## 非目标

- ④步 IMMEDIATE 硬截断路径（同 cause 闭集词汇，mode 差异归既有取消测试）。

## 测试

1 用例（3 连跑稳定）；core 全模块零回归。

## 兼容性

纯测试增量。
