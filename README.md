# Copycat

Burp Suite extension for copying HTTP requests/responses with header filtering.

## Release

Download the latest `Copycat.jar` from the [Releases](https://github.com/aleister1102/copycat/releases) page.

## Installation

1. Download `Copycat.jar` from releases or build from source: `gradlew jar`
2. In Burp Suite, go to Extensions > Installed > Add
3. Select `Copycat.jar` from `build/libs/` directory
4. Click Next and Close

## Usage

1. Navigate to any Burp Suite tab (Proxy, Repeater, Intruder, Target)
2. Right-click on any HTTP request or response
3. Select "Copy Request/Response (Filtered)" from context menu
4. Paste the filtered content wherever needed

## Configuration

### Accessing Settings

1. In Burp Suite, go to Extensions > Installed
2. Find "Copycat" and click the gear icon, or
3. Go to Settings > Extensions > Copycat

### Configuring Header Exclusions

1. In the settings panel, modify the "Excluded Headers" text field
2. Use comma-separated patterns (regex supported)
3. Click "Save" to apply changes

### Pattern Examples

| Pattern | Description | Matches |
|---------|-------------|---------|
| `content-.*` | All content headers | content-length, content-type, content-encoding |
| `x-.*` | All X- headers | x-forwarded-for, x-real-ip, x-custom |
| `(?i)cookie` | Case-insensitive cookie | Cookie, COOKIE, cookie |
| `sec-.*` | Security headers | sec-ch-ua, sec-fetch-site, sec-websocket-key |

### Default Excluded Headers

```
content-length, transfer-encoding, connection, host, accept-encoding, user-agent, sec-.*
```