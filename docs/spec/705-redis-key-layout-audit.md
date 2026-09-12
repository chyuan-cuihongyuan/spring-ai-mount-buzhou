# 705 — Redis 键命名空间碰撞审计

> 来源：G 会话第 6 轮 = effort #705（03/08 键布局 × 341 fsck 思想；E R12 污染教训制度化）/ [T1010](../../.wayfinder/tickets/T1010-redis-key-audit.md) / [T1011](../../.wayfinder/tickets/T1011-redis-key-audit-verify.md) / impl 605。
> 换题注记：原池「流式背压水位读数」连撞 526/46§A/stall-watchdog——换入键治理方向。

## Problem

Redis store 的键布局分散在 RedisKeys（会话族）与各 backend（cb/lane/bulkhead 自拼前缀）。布局存在**结构性潜伏碰撞**（本轮勘察实测）：

1. `obs:spev:<spanId>`（span→event 索引，ZSET）≈ sessionId="spev"、spanId="spans" 时的 `obs:spev:spans`（会话→span 索引，ZSET）——**完全同串**，跨会话数据串写；
2. `obs:event:<eventId>`（event 正文索引）≈ sessionId="event" 族的 `obs:event:spans` 同串碰撞；
3. `lease:<sid>:seq`（fencing INCR 计数器，无 TTL）≈ sessionId="a:seq" 的租约 HASH `lease:a:seq`——**ZSET/HASH/STRING 类型冲突 + fencing 单调性被污染**（重启级事故形状）。

这些不是假设：sessionId 来自宿主应用、spanId 由框架生成——碰撞在多租户/迁移数据场景真实可达。而布局没有审计面，衰变只能在事故后可见。

## Solution

fsck 思想（341 StoreFsck 同型——衰变/风险在 restore 前可见）+ etcd 键空间纪律：

- `RedisKeyLayoutAudit`（store-redis，public，同包使用 RedisKeys）：
  - `audit(String prefix)` → `List<Finding>`：对布局做**结构性对抗模拟**——保留段（spev/event/sessions）注入 sessionId 形状、冒号后缀注入 lease 形状，产出每处碰撞 `Finding(kind, key, conflictWith, hint)`（kind ∈ SHAPE_COLLISION / RESERVED_SEGMENT / COLON_SUFFIX_TRICK）；
  - `reservedSegments()` → 保留段清单（读数面）；
  - `isSafeSessionId(String)` → 摄入前守卫谓词：非空、无 `:`（严于现状——冒号是后缀歧义根因）、不在保留段、无 glob 元字符（SCAN 转义已兜底但谓词从严供宿主选用）。
- 布局风险是**确定性的**（结构使然，与数据无关）——audit 结果可缓存可入启动体检/健康详情。

## User Stories

1. 升级体检：宿主升级前跑 audit——「当前版本布局有 3 处结构碰撞 + 安全谓词」一屏可读，决定是否启用谓词/等待键形状迁移版本。
2. 摄入守卫：宿主自建 sessionId 时先过 isSafeSessionId——碰撞类事故在门口拦截。

## Implementation Decisions

- 纯静态原语（无 bean 无 yml——fsck 同款「先原语后接线」节奏；健康面接线留后续轮）。
- Finding 的 key/conflictWith 用默认前缀 `buzhou:` 生成样例（宿主定制前缀时形状不变——审计用 prefix 参数重生成）。
- 不改键形状（破坏性迁移语义归大版本——本面只出证据+守卫）。

## Testing Decisions

- audit 断言三族 Finding 各自存在且 key/conflictWith 精确。
- isSafeSessionId："ok-1" 通过；"a:seq"/"spev"/""/null/"a*b" 拒绝。
- 定制前缀（"x:"）后碰撞 Finding 仍然成立（形状与 prefix 无关）。

## Out of Scope

- bulkhead/cb/lane/rl 单键 backend 模拟（无 sid 注入面）。
- 键形状修复（迁移语义）。
- JDBC store 布局审计（表结构天然无此问题）。

## Further Notes

本面是 E 会话 R12「前缀扫描族键设计先想污染面」教训的**制度化**：从坑备忘变成可执行审计。同类结构以后新键形状落地时先跑本审计。
