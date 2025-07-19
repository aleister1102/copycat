public class CopycatConstants {
    public static final String[] DEFAULT_PATTERNS = {
        "content-length", "transfer-encoding", "connection",
        "host", "accept-encoding", "user-agent", "sec-.*"
    };
    
    public static final String EXTENSION_NAME = "Copycat";
    public static final String TAB_NAME = "Copycat";
    
    public static final String DESCRIPTION_TEXT = """
            Configure which headers to exclude when copying HTTP requests and responses.
            Header patterns listed below will be filtered out from the copied content.
            Supports regular expressions (regex) for flexible pattern matching.""";
    
    public static final String INSTRUCTIONS_TEXT = """
            How to use Copycat:
            1. Right-click on any HTTP request/response in Proxy, Repeater, Intruder, or Target
               - Works in both list view and message editor tabs
            2. Select 'Copy Request (Filtered)' or 'Copy Response (Filtered)'
            3. The content will be copied to clipboard with excluded headers removed
            4. Use this settings panel to customize header exclusion patterns
            
            Regex Pattern Examples:
            • 'content-.*' - matches content-length, content-type, etc.
            • 'x-.*' - matches all X- headers
            • 'authorization' - exact match (literal string)
            • '(?i)COOKIE' - case-insensitive match for 'cookie'""";
    
    // Menu item labels
    public static final String COPY_REQUEST_LABEL = "Copy Request (Filtered)";
    public static final String COPY_RESPONSE_LABEL = "Copy Response (Filtered)";
    
    // Settings panel labels
    public static final String SETTINGS_TITLE = "Copycat - Settings";
    public static final String EXCLUDED_HEADERS_TITLE = "Excluded Headers Configuration";
    public static final String USAGE_INSTRUCTIONS_TITLE = "Usage Instructions";
    public static final String HEADER_PATTERN_LABEL = "Header pattern (regex):";
    
    // Button labels
    public static final String ADD_PATTERN_BUTTON = "Add Pattern";
    public static final String REMOVE_SELECTED_BUTTON = "Remove Selected";
    public static final String RESET_DEFAULTS_BUTTON = "Reset to Defaults";
    
    // Log messages
    public static final String EXTENSION_LOADED = "Copycat extension loaded successfully!";
    public static final String SETTINGS_REGISTERED = "Extension settings panel registered successfully in Settings > Extensions";
    public static final String SETTINGS_FALLBACK = "Settings panel registration failed, using suite tab instead: ";
    
    private CopycatConstants() {
        // Utility class - prevent instantiation
    }
}
