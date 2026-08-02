package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

public enum Dialect {
    H2("db/schema-h2.sql"),
    MYSQL("db/schema-mysql.sql"),
    POSTGRESQL("db/schema-postgresql.sql");

    private final String schemaResource;

    Dialect(String schemaResource) {
        this.schemaResource = schemaResource;
    }

    public String schemaResource() {
        return schemaResource;
    }
}
