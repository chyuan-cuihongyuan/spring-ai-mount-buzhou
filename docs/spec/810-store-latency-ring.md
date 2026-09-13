# 810 — 存储提交延迟环形读数

> 来源：H 会话第 11 轮 = effort #810 / [T1121](../../.wayfinder/tickets/T1121-store-latency-ring.md) / [T1122](../../.wayfinder/tickets/T1122-store-latency-ring-verify.md) / impl 563。
> 借鉴：etcd backend commit latency（≈50K star）。

## Problem

存储写慢（fsync 风暴/锁竞争/连接池枯竭）靠「感觉」：工具/hook 域有计时聚合（700 系），存储 append/load/findById 无样本环——「最近写一次要多久」不可查。

## Solution

`StoreLatencyRing` + `TimedMessageStore`（core.spi）：

- **样本环**：按操作名 FIFO 128 样本 + count/total/max + 最近秩 P50/P95 + ringFull。
- **装饰器**：TimedMessageStore 包装任意 MessageStore，三方法 nanoTime 计时（finally——异常也记录；异常语义零变更）。
- **有界**：操作名封顶 16（truncated 如实）；单环 128。
- **喂点灵活**：装饰器自动或装配侧手动 record（SummaryStore/StateStore 等其余 store 可复用环）。

## 兼容性

纯新增（装饰器需显式包装）；MessageStore 接口零变更。

## 诚实边界

窗口估计非全量分布；nanoTime 现场计时（不走 Clock——零开销取舍）；不告警不熔断。
