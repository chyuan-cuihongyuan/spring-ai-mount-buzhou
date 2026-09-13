# effort #810 — 存储提交延迟环形读数

- 会话：H 会话 800 系第 11 轮 ｜ spec [810](../../../docs/spec/810-store-latency-ring.md) ｜ 票 [T1121](../tickets/T1121-store-latency-ring.md)/[T1122](../tickets/T1122-store-latency-ring-verify.md) ｜ impl563
- 借鉴：etcd backend commit latency（etcd-io/etcd ≈50K star）+ pg_stat_statements 慢查询聚合

## 勘察（排重）

- ToolTimingAggregator/HookTimingAggregator（700 系）：工具/hook 域——存储域无计时面。
- WriteFailurePolicy（jdbc/redis）：写失败策略非延迟样本。
- EncryptingMessageStore：装饰器先例存在（包装 MessageStore）——计时装饰器同模式可行。
- grep -i `latency|commitTime`：WebhookDeliveryLatency 是 webhook 域——存储域缺位。

## 决定

`StoreLatencyRing`（core.spi，纯读数）：按操作名 FIFO 样本环（容量 128）+ count/total/max + 最近秩 P50/P95 + ringFull 标记；操作名封顶 16（truncated）；负值/空白名忽略。`TimedMessageStore`（core.spi 装饰器）：包装 MessageStore 三方法计时（nanoTime，异常 finally 照记、异常照抛）——行为零变更。喂点=装饰器或手动 record。

## 测试

1..100 分位精确(50/95)+total 5050/环挤老 138 灌入剩 11..138 的 P50=74 P95=132 精确账/未知 op+非法输入忽略/操作名封顶 16+recorded 含截断首笔/装饰器三方法透传+计时/异常也计时+照抛/双参 fail-fast——7 例全绿。

## 诚实边界

System.nanoTime 现场计时（Clock 注入归测试——装饰器内不走 Clock 以零开销）；样本环是「最近窗口」非全量分布（P95 为窗口估计）；不告警不熔断（读数面）。
