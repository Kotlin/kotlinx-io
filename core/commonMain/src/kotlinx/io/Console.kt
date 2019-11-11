package kotlinx.io

public expect object Console {
    public val input: Input
    public val output: Output
    public val error: Output
}
