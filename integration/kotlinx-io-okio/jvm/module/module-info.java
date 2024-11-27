module kotlinx.io.okio {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.io.core;
    requires transitive kotlinx.io.bytestring;
    // okio's module is automatic, so don't require it
    // requires transitive okio;

    exports kotlinx.io.okio;
}
