# effort #838 — 选举竞争读数

- 会话：H 会话 800 系第 38 轮 ｜ spec [838](../../../docs/spec/838-leader-election-stats.md) ｜ 票 [T1175](../tickets/T1175-leader-election-stats.md)/[T1176](../tickets/T1176-leader-election-stats-verify.md) ｜ impl590
- 借鉴：Redisson RedLock 竞争统计（redisson/redisson ≈36K star）；839 选举后端观测姊妹面

## 勘察（排重）

- RedisLeaderElector（331）：tryAcquireOrRenew/inspect——无竞争统计。
- G 会话 R30 选举租约观测：租约域（etcd lease 观测）——竞争烈度缺位。
- grep -i `contention|election.*count`：无命中。

## 决定

`LeaderElectionStats`（store-redis，纯记账）：record(Outcome)——四态 ACQUIRED/RENEWED/OTHER_HOLDER/LOST 原子计数+totalAttempts+contentionRatio（(OTHER_HOLDER+LOST)/总尝试，0 稳态趋 1 激烈）；null 忽略；空真 0。喂点=选举器包装/装配侧在 tryAcquireOrRenew 返回后归类喂入（不改选举行为）。

## 测试

四态计数+烈度 2/6 精确/稳态 50 次续期烈度 0/null 忽略+空真——3 例全绿。

## 诚实边界

四态归类由调用方判别（Leadership 形状语义归选举器）；本类不区分 scope（多 scope 由调用方分桶）；纯计数无时序（抖动检测可基于烈度时序自建）。
