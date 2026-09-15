# Spec 1845 — argv 预算门（effort #1845，R46）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2891–T2892，impl 1446）。借鉴：
> Linux execve ARG_MAX / MAX_ARG_STRLEN——参数超限不是性能问题是 E2BIG
> 直接拒跑；双闸先验后拼。

## Problem Statement

RunCommand 类工具的参数校验只有黑名单/字符集：参数总量与单参长度无闸——
模型生成超长 argv 时在 exec 层吃 E2BIG（错误晚、诊断差），或拼出巨型
命令行拖垮子进程——系统级约束没有前移到参数校验层。

## Solution

`ArgvBudgetGate`（buzhou-tools，静态纯函数）：

- `totalBytes(args)` 字节账（每参字符数 + 1 计 NUL 分隔）；
- `verify(args, totalBudget, singleArgCap)` → 三态 `FIT / OVER_TOTAL /
  OVER_SINGLE_ARG`：**单参闸先于总量闸**（单参超限诊断价值高——直接指出
  哪类参数病）；边界含上（== 预算/== 上限均 FIT）；
- 默认闸参常量（1 MiB 总量 / 128 KiB 单参——ARG_MAX 量级）。

## User Stories

1. 作为工具实现者，拼命令行前 verify → OVER_SINGLE_ARG 直接回结构化
   错误「参数过长」，不用等 exec 层 E2BIG。
2. 作为安全审计者，总量闸封顶巨型 argv 注入面（参数炸弹）。
3. 作为框架宿主，闸参可调（嵌入式场景缩预算），纯校验零执行。

## Implementation Decisions

- 纯校验不执行（命令执行归宿主）；字节口径「字符数+1」显式入档
 （Java String 字符 ≠ UTF-8 字节，预算口径由调用方放大折算）。

## Testing Decisions

- 字节账含 NUL；三态+先序+边界含上；畸形三型 fail-fast。

## Out of Scope

- 不做 UTF-8 精确字节数（口径折算归宿主）；不执行命令。

## Further Notes

- 与 CommandBlacklist 正交：那是内容黑名单，这是体量双闸。
