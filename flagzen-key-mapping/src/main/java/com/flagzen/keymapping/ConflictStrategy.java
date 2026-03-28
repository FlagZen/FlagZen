package com.flagzen.keymapping;

/**
 * Strategy for handling conflicts when multiple environment variables
 * map to the same flag key.
 *
 * <p>The default strategy is determined by parser/formatter cardinality:
 * <ul>
 *   <li>1 parser x 1 formatter = WARN</li>
 *   <li>N parsers x 1 formatter = WARN</li>
 *   <li>1 parser x N formatters = WARN</li>
 *   <li>N parsers x N formatters = ERROR</li>
 * </ul>
 */
public enum ConflictStrategy {

    /**
     * Log a warning and continue operating. The last mapping wins.
     */
    WARN,

    /**
     * Reject construction with an {@link IllegalStateException}.
     */
    ERROR
}
