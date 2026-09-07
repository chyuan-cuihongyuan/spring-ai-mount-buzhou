# Spec 303 — 序号围栏跨重启持久纪元（effort #303）

> wayfinder map：`.wayfinder303/MAP.md`（T597–T598）。借鉴：Kafka producer
> epoch（idempotent producer generation——重启显式化，重启不再靠猜）。

## Problem Statement

围栏（159）把「seq 变小」猜测为发送方重启（RESET 裁决）——但 seq 倒退同样
可能是重放或截断（安全语义完全不同的场景）；且发送侧信封 seq 纯内存计数，
重启复位，接收方无从分辨。跨重启的序号语义缺一个显式的「代」概念。

## Solution

**发送侧持久纪元**：`WebhookOutbox` 启动期从合成会话 `meta.epoch` 读取上次
纪元，新纪元 = max(持久值+1, 启动墙钟毫秒) 后回写；信封新增 `epoch` 字段
（恒正）。存储写失败降级墙钟值（实际不撞号）。

**围栏纪元感知**：`SequenceFence.observe(subscriptionId, epoch, seq)` 三参
重载（两参兼容委派 = 无纪元旧路径）。判定矩阵：

| 场景 | 裁决 | 基线 |
|------|------|------|
| 纪元 > 基线纪元 | RESET（显式新纪元） | 重置为新纪元 |
| 纪元 = 基线纪元，seq 连续/跳号/重投 | CONTINUE / GAP / DUPLICATE | 既有语义 |
| 纪元 = 基线纪元，seq 倒退 | RESET（异常防御性重基线） | 重置 |
| 纪元 < 基线纪元 | STALE（旧纪元迟到——安全丢弃） | 不动 |
| 无纪元（旧发送方） | 原 seq 推断路径 | 逐位不变 |

## User Stories

1. 作为接收方，RESET 裁决只在发送方真的重启时出现——重放/截断不再被误判
   为「无所谓的重启」。
2. 作为接收方，重启期间滞留的旧纪元重试投递判 STALE——安全丢弃不污染新
   纪元基线。
3. 作为旧接收方，无 epoch 信封走原路径——升级零破坏。

## Implementation Decisions

- 纪元存 outbox 合成会话 `meta.epoch`（既有持久面复用，零新存储）。
- 发送侧 seq 每纪元从 1 起（纪元化后无需跨纪元续号——纪元就是续号替代物）。

## Testing Decisions

- `SequenceFenceEpochTest`：五象限判定矩阵 + 旧路径兼容。
- `WebhookForwarderEpochTest`（JDK HttpServer 收件，对齐
  WebhookEventForwarderTest 手法）：首启信封含 epoch E1>0 且 seq=1；同
  store 重建 forwarder → epoch E2>E1（持久递增）；旧接收方无感知（epoch
  多余字段忽略）。

## Out of Scope

- 接收方基线持久化（接收侧自决）；outbox 内部排序 seq（spec 24 语义不动）。

## Further Notes

- 投递可靠族：outbox（24）/ 退避抖动（50）/ lag 面（135）/ 多 sink（151）/
  围栏（159）/ **持久纪元（本轮）**。
