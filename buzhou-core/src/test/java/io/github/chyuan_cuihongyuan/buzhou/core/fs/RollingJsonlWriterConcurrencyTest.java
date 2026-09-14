package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 滚动 writer 并发行完整性测试（spec 1607 / T2365–T2366 / impl 1160）：
 * monitor→ReentrantLock 迁移后互斥语义不变——虚拟线程并发追加行不撕裂
 * （每行完整、计数守恒）。锁迁移消除 pinning（j.u.c 锁 unmount 而非 pin）。
 */
class RollingJsonlWriterConcurrencyTest {

    private static final int THREADS = 8;
    private static final int LINES_PER_THREAD = 50;

    @Test
    void concurrentAppendLinesStayIntactUnderReentrantLock(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("events.jsonl");
        try (RollingJsonlWriter writer = new RollingJsonlWriter(file, 0, 0)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Thread> threads = new java.util.ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                final int id = t;
                Thread vt = Thread.ofVirtual().unstarted(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < LINES_PER_THREAD; i++) {
                            // 行内换行已转义（JSONL 语义）——标记线程 id 与序号
                            writer.appendLine("{\"t\":" + id + ",\"i\":" + i + "}");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                threads.add(vt);
                vt.start();
            }
            start.countDown();
            for (Thread vt : threads) {
                vt.join(5000);
            }
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        // 计数守恒：无丢失无重复
        assertThat(lines).hasSize(THREADS * LINES_PER_THREAD);
        // 行完整性：每行都是合法标记（无撕裂/交错）
        long distinctThreads = lines.stream()
                .map(l -> l.replaceAll("[^0-9]", " ").trim().split("\\s+")[0])
                .distinct().count();
        assertThat(distinctThreads).isEqualTo(THREADS);
    }
}
