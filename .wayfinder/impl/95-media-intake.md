# impl-95 — MediaIntake 字节摄取

**What to build:** 字节→spill 落盘→MediaRef（URI 引用）闭环，补 URI-only 输入面的字节来源。

**Blocked by:** None

**Status:** done

- [x] MediaIntake（intake/readBack 二进制 Latin-1 无损 + intakeText/readBackText UTF-8）
- [x] spill 语义全沿用（配额/原子写/级联删；fork 引用兼容）
- [x] 测试：全字节谱往返/文本通道/级联删除悬垂报错/空字节拒绝——spill 116/116 绿；spec 35 §C

## Done

commit：见 git log（impl-95）。
