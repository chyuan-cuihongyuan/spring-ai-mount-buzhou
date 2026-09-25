# impl 2231 — T 会话 T31 Buddy Allocator 伙伴分配器（spec 6030 / T6259–T6260 / T31）

纵切片：BuddyAllocator（core/memory）——2 的幂分裂/伙伴
逐级合并 + TreeSet 闲链确定性。

- 验证：`mvn -pl buzhou-core test -Dtest='BuddyAllocatorTest'` 全绿（MVN_EXIT=0）。
