# impl 2114 — R 会话 R14 尺寸分层合并挑选（spec 4013 / T6027–T6028 / R14）

纵切片：SizeTieredMergePicker（core/cleanup）——均值分桶 + 阈值
双门 + 多桶竞选。

- 验证：`mvn -pl buzhou-core test -Dtest='SizeTieredMergePickerTest'` 全绿。
