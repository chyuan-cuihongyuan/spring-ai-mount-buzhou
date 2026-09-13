# effort #819 — Token 估算校准偏差审计

- 会话：H 会话 800 系第 20 轮 ｜ spec [819](../../../docs/spec/819-estimator-calibration.md) ｜ 票 [T1139](../tickets/T1139-estimator-calibration.md)/[T1140](../tickets/T1140-estimator-calibration-verify.md) ｜ impl572
- 借鉴：预测校准思想（估算 vs 真值系统性偏差量化；tiktoken 估算实践）
- **换题注记**：原 R20 PII 置信分布经勘察不成立（PiiDetector 正则型无分数维；PiiHitStats 已有类型计数）——换入估算校准题（估算 vs 模型真实 usage 的偏差缺口）。

## 勘察（排重）

- CharHeuristicTokenEstimator：启发式估算——无校准面。
- JudgeCalibration：eval 判定域——token 估算域缺位。
- grep -i `calibrat`：仅 JudgeCalibration 命中。

## 决定

`EstimatorCalibrationAudit`（core.spi）：record(estimated, actual) 成对入账（actual≤0/负值忽略）——相对误差=(est−act)/actual（正=高估预算松、负=低估预算紧，口径声明）；累计 meanRelativeError+biasOverRatio+biasUnderRatio+近窗 128 对 |误差| 最近秩 P95；empty 全 0 空真。喂点=afterModel usage 回报处（装配侧）。

## 测试

+0.2/−0.1/0 三对误差与占比精确/P95=94（{0..99} 各一次升序 95 位——首跑 95 笔误修正）/三形态脏对忽略/空真五断言/近窗挤出后累计 pairs 仍 128+20——5 例绿。

## 诚实边界

校准是事后审计（不改估算器参数——自适应估算器留位）；actual 口径=模型 usage 回报（多模型混布由调用方分桶喂）；字符估算本征粗粒度（审计给的是缺口量化非修复）。
