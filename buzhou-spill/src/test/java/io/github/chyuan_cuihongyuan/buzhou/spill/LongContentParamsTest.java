package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LongContentParamsTest {

    @SuppressWarnings("unused")
    static class Fixture {
        public void writeFile(String path, @LongContentParam String content, String contentPath) {
        }

        public void replace(String path, String oldStr, @LongContentParam String newStr,
                            String newStrPath) {
        }

        public void script(@LongContentParam String scriptContent, String scriptFilePath) {
        }

        public void explicit(@LongContentParam(pathParam = "srcFile") String body, String srcFile) {
        }

        public void noPathParam(@LongContentParam String text) {
        }

        public void nothing(String plain) {
        }
    }

    private Method method(String name, Class<?>... types) throws Exception {
        return Fixture.class.getMethod(name, types);
    }

    @Test
    void contentSuffixPairsWithContentPath() throws Exception {
        List<LongContentParamPair> pairs = LongContentParams.scan(
                method("writeFile", String.class, String.class, String.class));
        assertThat(pairs).containsExactly(new LongContentParamPair("content", "contentPath"));
    }

    @Test
    void plainNamePairsWithXxxPath() throws Exception {
        List<LongContentParamPair> pairs = LongContentParams.scan(
                method("replace", String.class, String.class, String.class, String.class));
        assertThat(pairs).containsExactly(new LongContentParamPair("newStr", "newStrPath"));
    }

    @Test
    void decoStyleScriptContentPairsWithScriptFilePath() throws Exception {
        List<LongContentParamPair> pairs = LongContentParams.scan(
                method("script", String.class, String.class));
        assertThat(pairs).containsExactly(new LongContentParamPair("scriptContent", "scriptFilePath"));
    }

    @Test
    void explicitPathParamWins() throws Exception {
        List<LongContentParamPair> pairs = LongContentParams.scan(
                method("explicit", String.class, String.class));
        assertThat(pairs).containsExactly(new LongContentParamPair("body", "srcFile"));
    }

    @Test
    void missingComplementaryPathParamYieldsNoPair() throws Exception {
        assertThat(LongContentParams.scan(method("noPathParam", String.class))).isEmpty();
        assertThat(LongContentParams.scan(method("nothing", String.class))).isEmpty();
    }
}
