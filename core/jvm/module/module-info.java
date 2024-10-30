module kotlinx.io.core {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.io.bytestring;

    exports kotlinx.io;
    exports kotlinx.io.unsafe;
}
