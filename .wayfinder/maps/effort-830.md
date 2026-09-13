# effort #830 — 审计树形健康读数

- 会话：H 会话 800 系第 31 轮 ｜ spec [830](../../../docs/spec/830-audit-tree-health.md) ｜ 票 [T1161](../tickets/T1161-audit-tree-health.md)/[T1162](../tickets/T1162-audit-tree-health-verify.md) ｜ impl583
- 借鉴：Certificate Transparency 树语义扩散（838 Merkle 树形状面——细高树=证明路径长=批量验证慢的量化依据）

## 勘察（排重）

- AuditMerkleTree：建树+包含证明——无形状健康判定。
- AuditChainHealth：hash 链完整性——非树形状。
- grep -i `treedepth|nextPow2|padding`：无命中。

## 决定

`AuditTreeHealthReadout`（guard.audit，纯函数）：analyze(leafCount)——深度 ⌈log2(n)⌉=32−numberOfLeadingZeros(n−1)/nextPow2/补位叶/满树判定（n=2^k 含 1）；0=空树深度 0 非满；负数归 0 不炸。叶数由调用方自树采集（零侵入）。

## 测试

七点边界账（0/1/2/3/4/5/8/9）+大树 1000（深度 10 补位 24）/负数归零——3 例全绿。

## 诚实边界

纯形状判定（不校验树内容——完整性归 AuditChainVerifier）；叶数采集归调用方；补位叶是概念位（AuditMerkleTree 实现是否复制填充由其内部定义——本类只报数学形状）。
