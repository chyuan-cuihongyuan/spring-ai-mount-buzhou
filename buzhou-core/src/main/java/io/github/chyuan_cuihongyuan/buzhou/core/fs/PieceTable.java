package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import java.util.ArrayList;
import java.util.List;

/**
 * Piece Table 文本缓冲（spec 6010 / T6221 / impl 2211）——
 * VSCode/Word 文本缓冲思想：**原稿只读 + 追加型增量缓冲**
 * ——片段（piece）表按序引用原稿或增量缓冲的区间拼装视图，
 * insert/delete 只增删/切分片段、从不搬动原文字符——每
 * 编辑复制全文（O(n) 编辑放大，大文档卡顿）的病解。
 * addedLength 只增不减（审计增量可见——增量缓冲只存新字符）。
 *
 * <p>与 RopeBuffer（spec 4044）同族不同面：权重树拼接 vs
 * 原稿+片表双缓冲。静态定构（同操作序列同片段表——确定性）。
 */
public final class PieceTable {

    private record Piece(boolean fromOriginal, int start, int length) {
    }

    private final String original;
    private final StringBuilder added = new StringBuilder();
    private final List<Piece> pieces = new ArrayList<>();
    private int length;

    /** 以原稿初始化（null fail-fast；空原稿允许）。 */
    public PieceTable(String original) {
        if (original == null) {
            throw new IllegalArgumentException("原稿非空");
        }
        this.original = original;
        this.length = original.length();
        if (!original.isEmpty()) {
            pieces.add(new Piece(true, 0, original.length()));
        }
    }

    /** 位置插入（null 串/越界 fail-fast；空串 no-op）。 */
    public void insert(int pos, String text) {
        if (text == null) {
            throw new IllegalArgumentException("插入文本非空");
        }
        if (pos < 0 || pos > length) {
            throw new IllegalArgumentException("插入位置越界: " + pos);
        }
        if (text.isEmpty()) {
            return;
        }
        int addedStart = added.length();
        added.append(text);
        length += text.length();
        int cur = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (pos == cur) {
                pieces.add(i, new Piece(false, addedStart, text.length()));
                return;
            }
            if (pos < cur + piece.length()) {
                int off = pos - cur;
                pieces.set(i, new Piece(piece.fromOriginal(), piece.start(), off));
                pieces.add(i + 1, new Piece(false, addedStart, text.length()));
                pieces.add(i + 2, new Piece(piece.fromOriginal(),
                        piece.start() + off, piece.length() - off));
                return;
            }
            cur += piece.length();
        }
        pieces.add(new Piece(false, addedStart, text.length()));
    }

    /** 区间删除（越界/负长 fail-fast；零长 no-op）。 */
    public void delete(int pos, int deleteLength) {
        if (deleteLength < 0) {
            throw new IllegalArgumentException("删除长度非负: " + deleteLength);
        }
        if (pos < 0 || pos + deleteLength > length) {
            throw new IllegalArgumentException("删除区间越界: [" + pos + ", "
                    + (pos + deleteLength) + ")");
        }
        if (deleteLength == 0) {
            return;
        }
        int remaining = deleteLength;
        int cur = 0;
        for (int i = 0; i < pieces.size() && remaining > 0; ) {
            Piece piece = pieces.get(i);
            int pieceEnd = cur + piece.length();
            if (pieceEnd <= pos) {
                cur = pieceEnd;
                i++;
                continue;
            }
            int cutStart = Math.max(pos, cur) - cur;
            int cutCount = Math.min(remaining, pieceEnd - Math.max(pos, cur));
            int retained = piece.length() - cutCount;
            if (retained == 0) {
                pieces.remove(i);
                remaining -= cutCount;
                continue;
            }
            if (cutStart == 0) {
                pieces.set(i, new Piece(piece.fromOriginal(),
                        piece.start() + cutCount, retained));
                cur += retained;
                i++;
            } else if (cutStart + cutCount == piece.length()) {
                pieces.set(i, new Piece(piece.fromOriginal(), piece.start(), cutStart));
                cur += retained;
                i++;
            } else {
                pieces.set(i, new Piece(piece.fromOriginal(), piece.start(), cutStart));
                pieces.add(i + 1, new Piece(piece.fromOriginal(),
                        piece.start() + cutStart + cutCount,
                        piece.length() - cutStart - cutCount));
                cur += retained;
                i += 2;
            }
            remaining -= cutCount;
        }
        length -= deleteLength;
    }

    /** 拼装文本视图。 */
    public String text() {
        StringBuilder out = new StringBuilder(length);
        for (Piece piece : pieces) {
            String source = piece.fromOriginal() ? original : added.toString();
            out.append(source, piece.start(), piece.start() + piece.length());
        }
        return out.toString();
    }

    /** 文本长度读数。 */
    public int length() {
        return length;
    }

    /** 片段数读数（结构可见）。 */
    public int pieceCount() {
        return pieces.size();
    }

    /** 增量缓冲长度读数（只增不减——编辑不复制的证据）。 */
    public int addedLength() {
        return added.length();
    }
}
