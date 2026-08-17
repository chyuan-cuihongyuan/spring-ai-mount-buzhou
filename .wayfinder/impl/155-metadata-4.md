# 155 — 配置元数据四批 + 绑定缺陷修复

**Parent:** spec 46–51 键 / [T187](../tickets/T187-metadata-4.md)

**Status:** done

- [x] 元数据 7 键入档：resilience 6（fallback.canary-enabled / fallback.weights / shadow.enabled /
  models / max-concurrent / daily-budget）+ core 1（core.stream-total-timeout，默认 10m、≤0 关）
- [x] 绑定验证 4 测试：canary/shadow 默认关 + yml 覆盖（shadow.models 同名 bean fail-fast 口径）+
  stream-total-timeout 默认 10m/5m/0 哨兵
- [x] **勘察纠偏（高严重度缺陷修复）**：Fallback/Circuit 多构造器嵌套 record 缺
  @ConstructorBinding → `buzhou.resilience.fallback.*` 与 `circuit.*` 全部 yml 键自 impl-57 起
  **静默不生效**（所有既有测试均编程式构造，从未暴露）。最小复现：Binder.bind 顶层 No value
  bound；带 fallback 键的上下文 fallback() 为 null。修复 = canonical 构造器补注解（与顶层
  record 同风格）；circuit 绑定回归测试补上。resilience 全套 104 绿。
