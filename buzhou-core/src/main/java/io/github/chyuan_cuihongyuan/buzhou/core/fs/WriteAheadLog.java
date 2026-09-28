package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * 写前日志（spec 7021 / U7243 / impl 2273）——PostgreSQL WAL/
 * Redis AOF 思想：**先落日志后动状态**——写侧为每条记录
 * 存 CRC32 校验和，重放全量重算比对（损坏 fail-fast 携带
 * LSN——静默吐坏数据的病解）；分段滚动（segmentSize 条/段）。
 * 进程内镜像段（落盘句柄由宿主持有——磁盘面明示不做，只做
 * 日志语义面）；LSN 单调递增（重放序=写入序，完全确定）；
 * 段负载快照暴露内部数组镜像（审计面——测试可注入损坏验证
 * 校验路径真实生效）。
 *
 * <p>与 SegmentLog（5028）同族不同面：滚动淘汰旧段 vs
 * 校验重放损坏检测。
 */
public final class WriteAheadLog {

    /** 记录（负载为内部数组镜像——审计/损坏注入面）。 */
    public static final class Record {
        final byte[] payload;
        final long checksum;

        Record(byte[] payload, long checksum) {
            this.payload = payload;
            this.checksum = checksum;
        }

        public byte[] payload() {
            return payload;
        }

        public long checksum() {
            return checksum;
        }
    }

    /** 段。 */
    public static final class Segment {
        final List<Record> records = new ArrayList<>();

        public List<Record> records() {
            return records;
        }
    }

    private final int segmentSize;
    private final List<Segment> segments = new ArrayList<>();
    private long lsn;

    /** segmentSize≥1（越域 fail-fast）。 */
    public WriteAheadLog(int segmentSize) {
        if (segmentSize < 1) {
            throw new IllegalArgumentException("段容量须为正: " + segmentSize);
        }
        this.segmentSize = segmentSize;
        segments.add(new Segment());
    }

    /** 追加记录（返回 LSN；null/空负载 fail-fast）。 */
    public long append(byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("负载非空");
        }
        Segment current = segments.get(segments.size() - 1);
        if (current.records.size() >= segmentSize) {
            current = new Segment();
            segments.add(current);
        }
        current.records.add(new Record(payload.clone(), checksumOf(payload)));
        return ++lsn;
    }

    /** 重放（全量重算校验比对；损坏 fail-fast 携带 LSN）。 */
    public List<byte[]> replay() {
        List<byte[]> out = new ArrayList<>();
        long sequence = 0;
        for (Segment segment : segments) {
            for (Record record : segment.records) {
                sequence++;
                if (checksumOf(record.payload) != record.checksum) {
                    throw new IllegalStateException(
                            "日志损坏——LSN " + sequence + " 校验不符");
                }
                out.add(record.payload.clone());
            }
        }
        return out;
    }

    private static long checksumOf(byte[] payload) {
        CRC32 crc = new CRC32();
        crc.update(payload);
        crc.update((byte) payload.length);
        return crc.getValue();
    }

    /** 末 LSN 读数。 */
    public long lastLsn() {
        return lsn;
    }

    /** 段数读数。 */
    public int segmentCount() {
        return segments.size();
    }

    /** 指定段镜像（越域 fail-fast）。 */
    public Segment segment(int index) {
        if (index < 0 || index >= segments.size()) {
            throw new IllegalArgumentException("段下标越域 [0," + segments.size() + "): " + index);
        }
        return segments.get(index);
    }

    /** 记录总数读数。 */
    public int totalRecords() {
        int total = 0;
        for (Segment segment : segments) {
            total += segment.records.size();
        }
        return total;
    }
}
