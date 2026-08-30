# impl-98 — 配置元数据补全

**What to build:** effort #6/#7 新配置键的 IDE 提示、默认值与废弃标记三面齐备。

**Blocked by:** T118/T119（新键定格）— 已闭合

**Status:** done

- [x] core：outbox-capacity / result-limit-chars / result-limit-overrides；queue-capacity 标 deprecated（replacement）
- [x] resilience：backoff-cap / half-open-success-threshold
- [x] skills：新建元数据（含 catalog-max-entries/catalog-cache-ttl）
- [x] JSON 校验 + 三模块编译绿；spec 21 增补节

## Done

commit：见 git log（impl-98）。
