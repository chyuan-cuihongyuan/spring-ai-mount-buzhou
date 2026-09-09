# Wayfinder Map — Buzhou 虚拟 key yml 装配（effort #123，A 会话第 18 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 148 fog「autoconfig yml 键」收口
> （原拟「错误签名健康面」已由上会话 spec85 落地——弃题换此）。

## Destination

buzhou.virtual-keys.active-key + limits.<key>=<硬顶> 两键即得 key 级预算闸：
registry bean + 预算闸接线 + 健康段（@ConditionalOnBean 自动出现）全链装配。

## Notes

- 校验分工：bind 期只验 limits ≥1（矩阵逐键绑定不炸）；「active-key 必须在
  limits」装配期 fail-fast 不带病上线；additional-spring-configuration-metadata
  手维护文件 + 矩阵 SKIPPED（Map 结构化面）+ 子键样例三件套登记。

## Decisions so far

- [yml 装配](../tickets/T511-vkeys-yml.md) — BuzhouVirtualKeyProperties +
  autoconfig 两 bean + tokenBudgetHook 5 参接线。

## Not yet specified

- 多实例共享 key 额度（Redis 后端——与 RateLimitBackend 族同演进路径）。

## Out of scope

- 成本面配额；动态换 key。

## Tickets

- [x] [T511 虚拟 key yml 装配](../tickets/T511-vkeys-yml.md)（impl-290）
- [x] [T512 收口提交](../tickets/T512-vkeys-yml-close.md)（impl-290）
