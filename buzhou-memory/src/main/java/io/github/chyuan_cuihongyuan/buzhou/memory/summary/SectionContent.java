package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import java.util.List;
/** spec 1529 / T2309：摘要段落内容载体——九段式中单段的标题与正文对。 */

public record SectionContent(String body, Form form, List<String> evidenceIds) {

    public enum Form {
        FULL,
        GIST
    }

    public SectionContent {
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }

    public static SectionContent full(String body) {
        return new SectionContent(body, Form.FULL, List.of());
    }

    public String render() {
        if (form == Form.GIST) {
            return body + (evidenceIds.isEmpty()
                    ? "" : "（证据：" + String.join(",", evidenceIds) + "）");
        }
        return body;
    }
}
