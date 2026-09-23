# Spec 5007 — Seqlock 序号锁（effort #5007，S8）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6115–T6116，impl 2158）。
> 借鉴：Linux 内核 seqlock（序号奇偶标写入期，读者重试达一致）。

## Problem Statement

读多写少小数据的并发读病：读写锁（读者互斥、写者饿死风险）
或全量加锁（读路径成本高）——**乐观读 + 写期奇偶标记面**
缺失。

## Solution

`SeqLock`（core/concurrent）：

- 序号奇偶语义：偶=稳定、奇=写入中；`writeBegin` 偶→奇
 （单写者串行，他写者 CAS 失败即退避）、`writeEnd` 奇→偶；
- 读者乐观读：`readBegin` 取序号（奇则重试）、读共享数据后
  `readEnd(observed)` 校验序号未变且为偶——失败即整读作废
  重试（无阻塞、无读者互斥）；
- 读数面：sequence()/isWriting()；
- fail-fast：writeEnd 传入偶序号 / 非本人序号 IAE。

## User Stories

1. 作为共享计数/配置作者，读者零阻塞乐观读，写者不饿死。
2. 作为审计作者，同操作序列同序号轨迹（确定性可回放）。

## Testing Decisions

- 序号奇偶算术（begin 奇/end 偶）；跨写校验失败；并发烟测
 （单写者连写二元组、读者重试至一致——观察到的对永不撕裂
  且终态可见）；嵌套写串行 CAS；畸形 writeEnd IAE。

## Out of Scope

- 不做多写者仲裁（单写者口径）；不做读者侧阻塞等待；不做
  结构化共享数据封装（裸序号面）。

## Further Notes

- 与 TicketLock 同族不同面：写者公平互斥 vs 读者乐观重试。
  Wave 2 第三件。
- 里程碑：S8/50（16%）。
