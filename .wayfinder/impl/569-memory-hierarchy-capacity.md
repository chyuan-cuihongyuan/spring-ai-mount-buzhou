# impl 569 — MemoryHierarchyCapacity（effort #816）

## 切片

- `buzhou-memory/src/main/java/.../memory/MemoryHierarchyCapacity.java` — 纯静态 analyze+layer 归级（LEVEL 常量公开）。
- `buzhou-memory/src/test/java/.../memory/MemoryHierarchyCapacityTest.java` — 4 例。

## 口径

- core-summary items 恒 max(1,1)=1——占位语义（单活跃版本）。
- capChars≤0 → record 内 capChars=null（隐藏哨兵 0——外部不 see 0）。

## 验证

mvn -pl buzhou-memory -am test -Dtest='MemoryHierarchyCapacityTest' → 4/4 绿；快照再生 1 新公共类型。
