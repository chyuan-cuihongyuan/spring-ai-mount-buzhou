# impl 2211 — T 会话 T11 Piece Table 文本缓冲（spec 6010 / T6221–T6222 / T11）

纵切片：PieceTable（core/fs）——原稿只读+追加增量片表；
delete 循环两处真缺陷由随机 oracle 钉住根治（①裁剪后 cur
未按保留长推进致后续片段坐标漂移；②split 右片段起点漏加
cutStart 致被删字符复活），并确立「删除区间随删随缩」的
remaining 口径。

- 验证：`mvn -pl buzhou-core test -Dtest='PieceTableTest'` 全绿（MVN_EXIT=0）。
