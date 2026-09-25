# impl 2203 — T 会话 T3 Treap 树堆（spec 6002 / T6205–T6206 / T3）

纵切片：Treap（core/concurrent）——BST+最小堆双不变量单旋
平衡 + 种子化 SplitMix64 + TreeMap 圣像。

- 验证：`mvn -pl buzhou-core test -Dtest='TreapTest'` 全绿（MVN_EXIT=0）。
