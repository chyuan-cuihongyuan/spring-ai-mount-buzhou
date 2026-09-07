# Wayfinder Map — Buzhou A/B 撞号台账归一（effort #153，A 会话第 48 轮）

> 文档轮：双会话并行期的编号冲突明细（收口轮据此归一，不再散落各 MAP）。

## Destination

A/B 两会话的 ticket/impl/spec 撞号一页账 + 归一裁定。

## 撞号明细（提交序）

- **T445-T446 双用**：B 侧 #87（spawn 优先级，ddc7323）与 A 侧 #86A（superstep
  通用原语，a31f3e7）——B 建票在先、A 提交在先。归一：B 保留 T445-T446；
  A 侧改记 T445a-T446a（语义别名，不重排历史提交）。
- **impl272 双用**：同上两轮。归一：B 保留 impl272；A 侧记 impl272a。
- **spec125 双文件**：A 侧 125-tenant-sandbox（d6a5b77）与 B 侧 redis 语义缓存
  （53f2ee8 同称 spec125）。归一：B 侧改称 spec125b（文件名不动，台账别名）。
- **impl275 双用**：A 租户沙箱 + B redis 缓存。B 侧记 impl275b。
- **impl277 双用**：A 归档清理（04f8566）+ B PII yml（9f2224c）。B 记 impl277b。
- **impl280 双用**：A 尾采样（90836a6）+ B outbox 滞后（4f702a4）。B 记 impl280b。
- **T471-T472 双用**：B 侧 redis 缓存与 A 侧清单导出（bb824d3）。A 侧改记
  T471x-T472x；此后 A 跳 T501+ 成功避让（B 占 T471-T500 段）。

## 归一裁定

历史提交不重写；台账以「先提交者保号、后到者加后缀别名」结案。此后单会话
序列自然无撞。

## Tickets

- [x] [T583 撞号台账](../tickets/T583-ab-ledger.md)（impl-320）
- [x] [T584 收口提交](../tickets/T584-ab-ledger-close.md)（impl-320）
