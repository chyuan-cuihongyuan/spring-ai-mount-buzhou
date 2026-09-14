# 1084 — fs 全链路四读面组合测试轮

> 来源：J 会话第 84 轮 = effort #1084（[T1623](../../.wayfinder/tickets/T1623-fs-chain-shape.md) / [T1624](../../.wayfinder/tickets/T1624-fs-chain-verify.md) / impl 836）。纯测试轮第三弹（R81/R82 先例）。

## Problem Statement

R45/R46/R47/R51 四读面在真实 fs 工作流中协同（写→读→越界→受控命令），各轮独立验证——**链路组合下的计数一致性与跨面对称恒等**无验证。

## 目标

新增 `FsChainReadoutTest`（buzhou-tools）：写（writes+bytesWritten）→ 读回（reads+bytesRead 对称）→ 越界读（read failures）→ 越界写（write failures）→ 黑名单命令（blacklist matches），断言四读面各自守恒保持 + 跨面字节对称恒等。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- run_command 执行侧（R52/R74 已分轴）。
- 其他域链路组合（按需另轮）。
