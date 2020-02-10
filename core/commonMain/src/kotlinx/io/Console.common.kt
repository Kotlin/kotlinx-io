package kotlinx.io

/**
 * [Console] incorporates all system inputs and outputs in a multiplatform manner.
 * All sources are open by default, ready to read from/write to, and cannot be closed.
 */
public expect object Console {
    /**
     * Standard input for the platform:
     *   * JVM -- `System.in`
     *   * Native -- input associated with `STDIN_FILENO`
     *   * JS -- not implemented
     */
    public val input: Input

    /**
     * Standard output for the platform:
     *   * JVM -- `System.out`
     *   * Native -- output associated with `STDOUT_FILENO`
     *   * JS -- output associated with `console.log`
     */
    public val output: Output

    /**
     * Standard error output for the platform:
     *   * JVM -- `System.err`
     *   * Native -- output associated with `STDERR_FILENO`
     *   * JS -- output associated with `console.error`
     */
    public val error: Output
}
