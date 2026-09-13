# effort #801 — Redis 大值审计

- 会话：H 会话 800 系第 2 轮 ｜ spec [801](../../../docs/spec/801-redis-value-size-audit.md) ｜ 票 [T1103](../tickets/T1103-redis-value-size-audit.md)/[T1104](../tickets/T1104-redis-value-size-audit-verify.md) ｜ impl554
- 借鉴：Redis BIGKEY 治理（redis/redis ≈68K star；redis-cli --bigkeys 与阿里云大 key 诊断惯例）

## 勘察（排重）

- RedisKeyLayoutAudit（705）：键形状<b>碰撞</b>审计——无字节量维度。
- SessionExportSizeAudit（738）：导出体积归因——导出域非 Redis 存储域。
- EventPayloadSizeAudit（732）：事件 payload——obs 单族非全布局。
- grep -i `bigkey|strlen|valueBytes`：无命中——大值族缺位。

## 决定

`RedisValueSizeAudit`（store-redis，纯函数）：对采样 (键→字节) 归族（idx/msg/msgid/sum/state/statekeys/lease/obs/semvec/other 前缀判定）+ WARN≥阈值/CRIT≥2× 两档定级 + Top32 降序 + 按族字节聚合 + 每族治理提示。采样（SCAN+STRLEN 折算）归调用方——判定脑与连接解耦（705「证据面不改行为」口径），确定性可入健康面。

## 测试

族归类 11 例/定级+排名+族聚合含低于阈值计入/Top 封顶/脏样本跳过+空真/阈值 fail-fast——5 例全绿。

## 诚实边界

纯函数不做采样（live SCAN 归运维侧——不做连接型审计是刻意的确定性取舍）；字节为采样侧口径（STRLEN 对 LIST/HASH 为整体折算由采样方决定）；未识别前缀归 other 不猜测。
