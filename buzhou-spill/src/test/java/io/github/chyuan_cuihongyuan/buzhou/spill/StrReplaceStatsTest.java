package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1054 / impl 806：str_replace 编辑判定读面——唯一命中成功、缺参/缺文件/
 * notFound/ambiguous 四拒绝桶、catch 兜底、六桶守恒恒等式、resetForTest 归零。
 */
class StrReplaceStatsTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void reset() {
        StrReplaceTool.resetForTest();
    }

    private StrReplaceTool tool() {
        return new StrReplaceTool(new FileSandbox(tmp, List.of()));
    }

    private void writeFile(String name, String content) throws Exception {
        Files.writeString(tmp.resolve(name), content);
    }

    @Test
    void uniqueReplaceCountsSuccess() throws Exception {
        writeFile("a.txt", "hello world");
        String out = tool().call(
                "{\"path\":\"a.txt\",\"oldStr\":\"hello\",\"newStr\":\"hi\"}");
        assertThat(out).contains("替换成功");

        StrReplaceTool.StrReplaceStats stats = StrReplaceTool.stats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.successes()).isEqualTo(1);
        assertThat(stats.totalRejects()).isZero();
    }

    @Test
    void missingParamsCountParamBucket() {
        tool().call("{\"path\":\"a.txt\",\"oldStr\":\"x\"}"); // 缺 newStr
        tool().call("{\"path\":\"a.txt\",\"newStr\":\"y\"}"); // 缺 oldStr

        assertThat(StrReplaceTool.stats().paramRejects()).isEqualTo(2);
    }

    @Test
    void missingFileCountsItsBucket() {
        String out = tool().call(
                "{\"path\":\"ghost.txt\",\"oldStr\":\"x\",\"newStr\":\"y\"}");
        assertThat(out).contains("目标文件不存在");

        assertThat(StrReplaceTool.stats().missingFileRejects()).isEqualTo(1);
    }

    @Test
    void notFoundCountsItsBucket() throws Exception {
        writeFile("b.txt", "content");
        String out = tool().call(
                "{\"path\":\"b.txt\",\"oldStr\":\"absent\",\"newStr\":\"z\"}");
        assertThat(out).contains("未找到待替换原文");

        assertThat(StrReplaceTool.stats().notFoundRejects()).isEqualTo(1);
    }

    @Test
    void ambiguousCountsItsBucket() throws Exception {
        writeFile("c.txt", "dup dup dup");
        String out = tool().call(
                "{\"path\":\"c.txt\",\"oldStr\":\"dup\",\"newStr\":\"x\"}");
        assertThat(out).contains("不唯一");

        assertThat(StrReplaceTool.stats().ambiguousRejects()).isEqualTo(1);
        assertThat(StrReplaceTool.stats().successes()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() throws Exception {
        writeFile("d.txt", "one target end");
        writeFile("e.txt", "dup dup");
        StrReplaceTool tool = tool();
        tool.call("{\"path\":\"d.txt\",\"oldStr\":\"target\",\"newStr\":\"hit\"}"); // 成功
        tool.call("{\"path\":\"d.txt\",\"oldStr\":\"\",\"newStr\":\"y\"}");          // 参数拒
        tool.call("{\"path\":\"ghost.txt\",\"oldStr\":\"x\",\"newStr\":\"y\"}");     // 缺文件拒
        tool.call("{\"path\":\"d.txt\",\"oldStr\":\"nope\",\"newStr\":\"y\"}");      // notFound 拒
        tool.call("{\"path\":\"e.txt\",\"oldStr\":\"dup\",\"newStr\":\"x\"}");       // ambiguous 拒

        StrReplaceTool.StrReplaceStats stats = StrReplaceTool.stats();
        assertThat(stats.attempts()).isEqualTo(5);
        assertThat(stats.attempts()).isEqualTo(stats.successes() + stats.totalRejects());
        assertThat(stats.successes()).isEqualTo(1);
        assertThat(stats.paramRejects()).isEqualTo(1);
        assertThat(stats.missingFileRejects()).isEqualTo(1);
        assertThat(stats.notFoundRejects()).isEqualTo(1);
        assertThat(stats.ambiguousRejects()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() throws Exception {
        writeFile("f.txt", "v");
        tool().call("{\"path\":\"f.txt\",\"oldStr\":\"v\",\"newStr\":\"w\"}");
        assertThat(StrReplaceTool.stats().attempts()).isEqualTo(1);

        StrReplaceTool.resetForTest();

        StrReplaceTool.StrReplaceStats stats = StrReplaceTool.stats();
        assertThat(stats.attempts()).isZero();
        assertThat(stats.successes()).isZero();
        assertThat(stats.totalRejects()).isZero();
    }
}
