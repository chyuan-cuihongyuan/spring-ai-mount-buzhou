# effort #734 — 数据集指纹变更信号（82 扩散）

- 会话：G 会话 700 系第 35 轮 ｜ spec [734](../../../docs/spec/734-fingerprint-change-signal.md) ｜ 票 [T1068](../tickets/T1068-fingerprint-change.md)/[T1069](../tickets/T1069-fingerprint-change-verify.md) ｜ impl634
- 借鉴：—（82 指纹入档的消费信号）

## 勘察（排重）
- 82 指纹只入档；EvalRunDiff 是显式两 run 对比——「最近一次 run 的数据集变了」无即时信号。

## 决定
EvalRunner 内 `checkFingerprintChange`：当前 run 指纹 vs 最近一次历史 run（早于本次）——不同置位 lastFingerprintChanged()+fingerprint.changed 计数+INFO；首跑 false。diff 明细归 EvalRunDiff（诚实边界）。

## 测试
首跑 false/增项置位/稳定复跑复位。
