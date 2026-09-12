# 622 — 归档/还原每会话互斥

> 来源：F 会话第 23 轮 = effort #600 / [T894](../../.wayfinder/tickets/T894-archiver-mutex-shape.md) / [T895](../../.wayfinder/tickets/T895-archiver-mutex-verify.md) / impl 475。思想：读-改-写竞态的最小收口是关键段互斥（先于更重的重设计）。

## 背景

archive 走 saga（CompensatingBatch，全局事务锁串行）；restore 直写 store 旁路事务。并发 archive+restore 交错：restore 先读走归档键并写回活数据，archive 随后级联删除刚还原的活数据——两边都「成功」但数据只剩归档键已被删的一份（丢失窗）。

## 目标

SessionArchiver 每会话互斥：archive/restore 同会话串行。

## 非目标

- 不改 CompensatingBatch/UnitOfWork 事务面（archive 全局串行是既有瓶颈——雾区：per-session 事务化）。
- purgeExpired 不加锁（只动归档键，不与活数据交错）。

## 设计

条目对象锁（ConcurrentHashMap computeIfAbsent）；archive/restore 包 synchronized。

## 测试

3 用例（withContributor 门/探针——SessionCleaner final）：并发双归档单胜 / archive 阻塞中 restore 被挡且完成后还原在 / 跨会话无死锁。

## 兼容性

行为面纯收紧（丢失窗关闭）；单线程调用零变化。
