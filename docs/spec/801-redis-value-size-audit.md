# 801 — Redis 大值审计

> 来源：H 会话第 2 轮 = effort #801 / [T1103](../../.wayfinder/tickets/T1103-redis-value-size-audit.md) / [T1104](../../.wayfinder/tickets/T1104-redis-value-size-audit-verify.md) / impl 554。
> 借鉴：Redis BIGKEY 治理（redis-cli --bigkeys；阿里云大 key 诊断惯例）。

## Problem

Redis 后端膨胀（超长消息正文、未压实的摘要、永生状态值、超大向量桶）只会在 OOM 或延迟抖动时被发现：705 审计键形状碰撞、738 审计导出体积、732 审计事件 payload——「哪个命名空间的哪个键在吃字节」无结构化面。

## Solution

`RedisValueSizeAudit`（store-redis，纯函数）：

- **族归类**：`familyOf(key)` 前缀判定九族（idx/msg/msgid/sum/state/statekeys/lease/obs/semvec），未识别归 other 不猜测。
- **定级**：WARN ≥ warnBytes、CRIT ≥ 2×warnBytes（大 key 惯例两档简化）。
- **排名**：超阈值键按字节降序 Top32（有界纪律）。
- **族聚合**：bytes/keys/overThreshold 三口径按族字节降序；低于阈值的键也计入族聚合（族级膨胀可见）。
- **治理提示**：每族一条人话 hint（如 msg→查 Spill 外置阈值、sum→查微压缩、state→查 StateTtlCoverage）。
- **采样解耦**：输入即事实 (键→字节)——live SCAN+STRLEN 归调用方；脏样本（null/负值）跳过不计。

## 兼容性

纯新增静态工具类，零装配变化；阈值由调用方传入（无 yml 键——健康面接线留位）。

## 诚实边界

纯函数无采样（确定性换便利——连接型审计是刻意的职责切分）；字节口径由采样侧决定（LIST/HASH 整体折算）；report.sampledKeys 计脏样本占位（输入即事实）。
