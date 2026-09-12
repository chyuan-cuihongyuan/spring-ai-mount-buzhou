# effort #710 — 全局 holdout 层

- 会话：G 会话 700 系第 11 轮 ｜ spec [710](../../../docs/spec/710-experiment-holdout.md) ｜ 票 [T1020](../tickets/T1020-experiment-holdout.md)/[T1021](../tickets/T1021-experiment-holdout-verify.md) ｜ impl610
- 借鉴：Statsig（文档惯例）holdout layers——全实验外的纯控制组，量度「实验总体净效应」

## 勘察（排重）

- 505/709：分桶+到期——**跨实验维度**无控制组语义；holdout 0 命中。
- A/A 抖动（514）是单实验内的安慰剂对照——不同层。

## 决定

ExperimentBucketer 构造器再扩 `holdoutPercent`（0..100，默认 0 零变化）：assign() 在到期判定后、落桶前判 `sha256("holdout|unitKey") mod 100 < holdoutPercent` → 返回 null+曝光计 `__holdout__` 独立桶+`buzhou.experiment.holdout` 计数；读数 `holdoutPercent()`。哈希与实验名无关（同 unit 全实验一致排除——层语义）。

## 测试

holdout 内 unit 全实验 null+__holdout__ 计数/holdout 外照常分桶/默认 0 零变化/构造校验（>100 拒绝）。

## 诚实边界

holdout 面固定不随实验动态调整；排除是 assign 层（已记录的旧曝光不回滚）。
