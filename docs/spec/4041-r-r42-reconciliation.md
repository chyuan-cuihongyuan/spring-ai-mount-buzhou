# Spec 4041 — R 系 R42 周期对账（effort #4041，R42）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6083–T6084，impl 2142）。
> 对账门四十二号：Wave 7 五新类型快照补登 + 全仓 verify + 台账核账。

## Problem Statement

Wave 7（R37–R41）五件新类型未入公共面快照——全仓 verify
快照门必红；档案计数需对齐。

## Solution

- 快照补登：1158→1163（ScalarKalmanFilter/HoltWintersIndex/
  TheilSenSlope/DdSketch——metrics×4 + MvccVisibility——
  transaction×1）；
- api-surface.md 同步 +5 行；CONTEXT 计数 1158→1163；
- **环境确定性清零第二击（O 会话 R78 先例；R36 已修 flush
  token 入队无界 put）**：R42 全仓 verify 再挂 19min 于同一
  管线测试——jstack 定位真因：测试自带
  `RecordingStore.saveEvents` **无界 `latch.await()`** + 裸
  300ms 时序断言（满载下 drain 未及取件、断言翻车，
  try-with-resources 的 close→flush 兜底 drain 再撞未释放
  latch 即永久挂死）。硬化：emit#1 后先 gate 等
  saveLatchStarted（证明 drain 已被卡，时序从赌变证）再填队
  + latch 限时 5s 自恢复；
  另 `UnsubscribedStreamTest.sequentialResubscription` 满载
  偶发撞单飞闸（block() 返回与 doFinally 闸释放跨线程竞速，
  单跑恒绿）——第二次订阅断言限时重试（语义不变）；
- 全仓 16 模块 `mvn verify`（三门全绿）；
- RSession4000LedgerAuditTest 台账核账（spec 4000–4040
  卅六轮四件套零缺位）。

## User Stories

1. 作为对账审计者，公共面快照与实际类型集一致——门不白设。
2. 作为后续轮作者，绿基线起跑。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳入 4000–4040）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：42/50=84%。
