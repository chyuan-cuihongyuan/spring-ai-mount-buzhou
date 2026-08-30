# 179 — 公共面快照测试

**Parent:** [T215](../tickets/T215-apisurface-snapshot.md)

**Status:** done

- [x] ApiSurfaceSnapshotTest：classpath 扫描（jar/classes 双形态）→ 非 internal public 类型
  全集（449 类型 × 14 模块）→ 黄金快照 docs/api-surface.snapshot.txt 比对
- [x] regenerateSnapshot 维护操作（-Dtest 单跑覆写）；更新流程注释入快照头
- [x] 实现期纠偏：Map 键结构 bug（模块名作键致每模块一项）——键改 FQN
- [x] 2 测试绿；面量下限 240 粗闸（449 现值）
