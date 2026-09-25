# impl 2205 — T 会话 T5 Monotonic Deque 单调队列（spec 6004 / T6209–T6210 / T5）

纵切片：MonotonicDeque（core/metrics）——定容窗严格递减
队列摊还 O(1) 最值 + 暴力扫圣像 + 越窗弹出逐值钉住。

- 验证：`mvn -pl buzhou-core test -Dtest='MonotonicDequeTest'` 全绿（MVN_EXIT=0）。
