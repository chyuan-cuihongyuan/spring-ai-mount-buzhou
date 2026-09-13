# impl 573 — GuardExemptionRegistry（effort #820）

## 切片

- `buzhou-guard/src/main/java/.../guard/GuardExemptionRegistry.java` — ConcurrentHashMap<mech\0subj, Entry>+惰性过期（remove(key, entry) 条件移除防竞态重复计数）+AtomicLong 双计数。
- `buzhou-guard/src/test/java/.../guard/GuardExemptionRegistryTest.java` — 5 例。

## 口径

- exempt 过期路径：remove 条件移除成功才计 expiredTotal（并发双查只计一次）。
- grant 满时也计 grantedTotal（登记意图如实）但拒绝入表。

## 验证

mvn -pl buzhou-guard -am test -Dtest='GuardExemptionRegistryTest' → 5/5 绿；快照再生 1 新公共类型。
