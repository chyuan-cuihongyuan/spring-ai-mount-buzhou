package io.github.chyuan_cuihongyuan.buzhou.spill;

public record RangeReadRequest(Mode mode, Integer offset, Integer limit,
                               String jsonPath, String cursor) {

    public enum Mode {
        BYTES,
        JSON,
        PAGE
    }

    public static RangeReadRequest bytes(int offset, int limit) {
        return new RangeReadRequest(Mode.BYTES, offset, limit, null, null);
    }

    public static RangeReadRequest json(String jsonPath) {
        return new RangeReadRequest(Mode.JSON, null, null, jsonPath, null);
    }

    public static RangeReadRequest page(String cursor, int limit) {
        return new RangeReadRequest(Mode.PAGE, null, limit, null, cursor);
    }
}
