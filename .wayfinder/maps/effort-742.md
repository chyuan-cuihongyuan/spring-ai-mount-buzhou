# effort #742 — sweepOrphans 保留计数读数

- 会话：G 会话 700 系第 43 轮 ｜ spec [742](../../../docs/spec/742-sweep-retained-readout.md) ｜ 票 [T1086](../tickets/T1086-sweep-retained.md)/[T1087](../tickets/T1087-sweep-retained-verify.md) ｜ impl643
- 借鉴：—（impl-38 孤儿扫描的保留证据面）

## 勘察（排重）

- sweepOrphans 返回 deleted 数——被 fork 引用而**保留**的孤儿数（衰减治理证据）只存在于局部变量，无读数。

## 决定

DiskSpillStore 加 `totalRetainedOrphans()`（累计保留）+`lastSweepRetained()`（最近一次保留数，-1=从未执行哨兵）——sweep 内计数入档。只读不清理语义变化。

## 测试

fork 引用保留计入+无引用清除分离/初始 -1 哨兵。
