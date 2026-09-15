# Spec 1861 — 写偏斜检测（effort #1861，R62）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2923–T2924，impl 1462）。借鉴：
> 数据库快照隔离 write skew 异象（经典「值班医生」反例）——读集相交、
> 写集不相交：写冲突检测放行而组合不变量被破。

## Problem Statement

并发事务只防写冲突（同键竞争）：两事务读同一支撑不变量的数据、各改
 各自的行——无同键竞争、双方均可提交，但组合结果破不变量（都看到对方
 在班而各自请假，班表空转）。快照隔离拦不住、要靠显式冲突桌——检测
 面缺位。

## Solution

`WriteSkewDetector`（core/transaction，静态纯函数）：

- `Transaction(id, readSet, writeSet)` 事实契约；
- `skewRisk(a, b)`：双方读写集均非空 且 读集相交 且 写集不相交——
  写冲突检测拦不住的组合风险；
- `scan(transactions)` 全对扫描（i&lt;j 入参序确定性）→ 风险对清单。

## User Stories

1. 作为并发作者，风险对清单直接映射到冲突桌（对读集加写锁或升级
   可串行化）——显式修复有依据。
2. 作为审计者，值班医生型反例在提交前显形——不变量破坏不再是「数据
   怎么互相矛盾了」的事后谜团。
3. 作为框架宿主，键口径（行键/会话键）自声明，纯检测不拦截。

## Implementation Decisions

- 纯检测不拦截（冲突桌/隔离升级归宿主）；写集相交即非偏斜（写冲突
  检测本来就能拦——口径显式）。

## Testing Decisions

- 经典医生反例为真；写冲突非偏斜；读不交/读写残缺非风险；全对扫描
  1 对+null 空；畸形两型 fail-fast。

## Out of Scope

- 不做冲突桌执行；不做幻读检测（谓词锁归未来静脉）。

## Further Notes

- 与 HalfMessageAudit 互补：那是跨系统原子性，这是隔离级别内的组合不变量。
