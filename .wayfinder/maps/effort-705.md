# effort #705 — Redis 键命名空间碰撞审计（换题轮）

- 会话：G 会话 700 系第 6 轮 ｜ spec [705](../../../docs/spec/705-redis-key-layout-audit.md) ｜ 票 [T1010](../tickets/T1010-redis-key-audit.md)/[T1011](../tickets/T1011-redis-key-audit-verify.md) ｜ impl605
- **换题注记**：原池「流式背压水位读数（Netty）」两连撞——WatermarkHealth（526）是上下文水位桥接、TTFT（46§A）已有 timer+事件、stall watchdog 覆盖慢流；按池规则换入备选方向（键治理），选题依据 E 会话 R12 前缀污染教训。
- 借鉴：Git fsck / 341 StoreFsck 同思想——布局衰变在事故前可见；etcd（≈47K）revision 语义文档化的键空间纪律

## 勘察（排重）

- RedisKeys（03/08）单一类管理会话族键；但 bulkhead/cb/lane/rl 等 backend **各自拼前缀**——布局无全局视图。
- 实际探测到潜伏碰撞：①`obs:spev:<spanId>`（span 事件索引）与 sessionId="spev"+spanId="spans" 的 `obs:spev:spans`（会话 span 索引）**完全同串**；②`obs:event:<eid>` 与 sessionId="event" 同理；③`lease:<sid>:seq`（fencing 计数器）与 sessionId="a:seq" 的租约键 `lease:a:seq` **同串**（类型冲突 HASH vs INCR——真事故形状）。
- grep KeyLayout/Namespace audit：零命中。

## 决定

`RedisKeyLayoutAudit`（store-redis，public；同包访问 package-private RedisKeys）：①`audit(prefix)` 结构性模拟对抗性 id（保留段/含冒号）产出 Finding(key, conflictWith, hint)——布局风险证据面；②`reservedSegments()`（sessions/spev/event/spans/seq…）+③`isSafeSessionId(sid)` 摄入前守卫谓词（非空/无冒号/不在保留段/无 glob 元字符）。纯读数+谓词，不改键形状（破坏性变更归后续大版本）。

## 测试

三族碰撞被找出（断言具体 Finding）/正常 id 谓词通过+对抗 id 拒绝/前缀定制后审计仍成立。

## 诚实边界

只审计 Redis 会话族布局（bulkhead/cb/lane 等单键 backend 无 sid 注入面，不模拟）；发现不自动修复（改键形状=迁移语义）；谓词从严（禁冒号——比现状更严，供宿主选用不强征）。
