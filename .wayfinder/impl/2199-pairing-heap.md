# impl 2199 — S 会话 S49 Pairing Heap 配对堆（spec 5048 / T6197–T6198 / S49）

纵切片：PairingHeap（core/concurrent）——O(1) meld 挂钩 +
extractMin 两趟合并 + TreeMap 多重集圣像。

- 验证：`mvn -pl buzhou-core test -Dtest='PairingHeapTest'` 全绿（MVN_EXIT=0）。
