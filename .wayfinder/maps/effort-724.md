# effort #724 — 会话状态 TTL 覆盖审计

- 会话：G 会话 700 系第 25 轮 ｜ spec [724](../../../docs/spec/724-state-ttl-coverage.md) ｜ 票 [T1048](../tickets/T1048-state-ttl-coverage.md)/[T1049](../tickets/T1049-state-ttl-coverage-verify.md) ｜ impl624
- 借鉴：S3/MinIO 生命周期审计——「无过期策略的对象」是存储泄漏主通道

## 勘察（排重）

- StateEntry.ttlTurns（Integer，null=永生）——永生键持续堆积是状态存储膨胀主通道；542 快照 TTL 迁移是单点修复，**覆盖面审计**无。grep -i ttlCoverage/永生：零命中（仅 RedisStoreProperties 注释提及）。

## 决定

`StateTtlCoverage`（core/cleanup 纯函数）：analyze(Map<String,StateEntry>)→Report——totalKeys/persistentKeys（ttlTurns=null）/ttlKeys+coverage（ttlKeys/total；total=0 空真 1.0）+byProducer 分行（哪个机制在写永生键——治理焦点）。

## 测试

混合 producer 覆盖率精确/空 map 空真/null fail-fast/producer 行字典序。

## 诚实边界

纯读数（补 TTL 动作归宿主）；轮次 TTL 的「过期判定」不在此（惰性过期在 store 读路径）。
