# Copycat - Burp Suite Extension

A powerful Burp Suite extension that allows you to copy HTTP requests and responses with customizable header filtering using regular expressions.

## Features

- **Smart Header Filtering**: Exclude unwanted headers when copying HTTP messages
- **Regex Pattern Support**: Use regular expressions for flexible header matching
- **Context Menu Integration**: Right-click to copy from Proxy, Repeater, Intruder, and Target tools
- **Configurable Settings**: Easy-to-use settings panel for managing exclusion patterns
- **Universal Compatibility**: Works in both list view and message editor tabs

## Installation

### Prerequisites
- Burp Suite Professional or Community Edition
- Java 21 or higher

### Building from Source

1. Clone this repository:
   ```bash
   git clone <repository-url>
   cd copycat
   ```

2. Build the JAR file:
   - **Windows**: `gradlew jar`
   - **Unix/Linux/macOS**: `./gradlew jar`

3. The compiled JAR will be available at `build/libs/Copycat.jar`

### Loading into Burp Suite

1. Open Burp Suite
2. Go to **Extensions > Installed**
3. Click **Add**
4. Select **Extension type**: Java
5. Click **Select file** and choose the `Copycat.jar` file
6. Click **Next** to load the extension

## Usage

### Basic Usage

1. Navigate to any HTTP request/response in:
   - Proxy > HTTP history
   - Repeater tabs
   - Intruder > Results
   - Target > Site map

2. Right-click on the request/response

3. Select from the context menu:
   - **Copy Request (Filtered)** - Copy the HTTP request with headers filtered
   - **Copy Response (Filtered)** - Copy the HTTP response with headers filtered

4. The filtered content is automatically copied to your clipboard

### Configuration

Access the settings through:
- **Settings > Extensions > Copycat** (Burp Suite 2025.5+)
- **Extensions > Copycat** tab (older versions)

#### Default Excluded Headers

By default, the following headers are excluded:
- `content-length`
- `transfer-encoding`
- `connection`
- `host`
- `accept-encoding`
- `user-agent`

#### Adding Custom Patterns

1. In the settings panel, enter a header pattern in the text field
2. Click **Add Pattern** or press Enter
3. The pattern supports regular expressions for flexible matching

#### Regex Pattern Examples

| Pattern | Description | Matches |
|---------|-------------|----------|
| `content-.*` | All content headers | content-length, content-type, content-encoding |
| `x-.*` | All X- headers | x-forwarded-for, x-real-ip, x-custom-header |
| `authorization` | Exact match | authorization (literal) |
| `(?i)cookie` | Case-insensitive | Cookie, COOKIE, cookie |
| `sec-.*` | Security headers | sec-fetch-site, sec-ch-ua |
| `cache-.*` | Cache headers | cache-control, cache-pragma |

#### Managing Patterns

- **Remove Selected**: Select patterns from the list and click to remove them
- **Reset to Defaults**: Restore the original default exclusion patterns
- **Pattern Validation**: Invalid regex patterns are treated as literal strings with a warning

## Technical Details

### How It Works

1. The extension registers context menu items for HTTP tools
2. When a copy action is triggered, it processes the HTTP message line by line
3. Header names are matched against configured regex patterns (case-insensitive)
4. Non-matching headers are included in the copied content
5. The filtered message is copied to the system clipboard

### Regex Processing

- Patterns are compiled using `java.util.regex.Pattern`
- Matching is performed case-insensitively
- Invalid regex patterns fall back to literal string matching
- Pattern validation occurs when adding new patterns

### Supported Tools

- **Proxy**: HTTP history and intercept
- **Repeater**: Request/response tabs
- **Intruder**: Attack results
- **Target**: Site map and scope

## Development

### Project Structure

```
copycat/
├── src/main/java/
│   └── Extension.java          # Main extension class
├── build.gradle.kts            # Build configuration
├── settings.gradle.kts         # Project settings
└── README.md                   # This file
```

### Key Components

- **Extension**: Main class implementing `BurpExtension`
- **CopycatContextMenuProvider**: Provides right-click menu items
- **CopycatSettingsPanel**: Configuration UI panel
- **CopyRequestAction/CopyResponseAction**: Action handlers for copying

### Building and Testing

1. Make code changes
2. Rebuild: `gradlew jar`
3. In Burp Suite, go to **Extensions > Installed**
4. Hold `Ctrl/⌘` and click the **Loaded** checkbox to reload the extension

## Troubleshooting

### Common Issues

**Extension not loading**
- Ensure Java 21+ is installed
- Check Burp Suite's extension error logs
- Verify the JAR file is not corrupted

**Context menu not appearing**
- Ensure you're right-clicking on HTTP requests/responses
- Check that the extension is loaded and enabled
- Verify you're in a supported tool (Proxy, Repeater, etc.)

**Regex patterns not working**
- Test your regex pattern in a regex validator
- Check the extension output for pattern validation errors
- Remember that invalid patterns are treated as literal strings

### Debug Information

The extension logs activity to Burp Suite's output:
- Pattern additions/removals
- Copy operations
- Regex validation errors

Access logs via **Extensions > Installed > [Extension] > Output**

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly with Burp Suite
5. Submit a pull request

## License

This project is open source. Please check the repository for license details.

## Support

For issues, feature requests, or questions:
- Create an issue in the repository
- Join the Burp Suite community discussions
- Check Burp Suite's extension documentation

---

**Note**: This extension is designed for security testing and research purposes. Use responsibly and in accordance with applicable laws and regulations.