import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.settings.SettingsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Extension implements BurpExtension {
    private MontoyaApi api;
    private Set<String> excludedHeaderPatterns;
    private Set<Pattern> precompiledPatterns;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.api = montoyaApi;
        montoyaApi.extension().setName("Copycat - HTTP Message Copier");

        // Initialize default excluded header patterns (regex supported)
        excludedHeaderPatterns = new HashSet<>(Arrays.asList(
                "content-length", "transfer-encoding", "connection",
                "host", "accept-encoding", "user-agent", "sec-.*"));

        // Precompile patterns during initialization
        precompiledPatterns = new HashSet<>();
        for (String pattern : excludedHeaderPatterns) {
            try {
                precompiledPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                // Fallback to literal pattern if compilation fails
                precompiledPatterns.add(Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE));
            }
        }

        // Register context menu provider
        montoyaApi.userInterface().registerContextMenuItemsProvider(new CopycatContextMenuProvider());

        // Register keyboard shortcut for quick copy
        registerKeyboardShortcuts();

        // Register as extension settings panel (Montoya API 2025.5+)
        try {
            montoyaApi.userInterface().registerSettingsPanel(new CopycatSettingsPanel());
            montoyaApi.logging()
                    .logToOutput("Extension settings panel registered successfully in Settings > Extensions");
        } catch (Exception e) {
            // Fallback to suite tab if settings panel registration fails
            montoyaApi.logging()
                    .logToOutput("Settings panel registration failed, using suite tab instead: " + e.getMessage());
            CopycatSettingsPanel settingsPanel = new CopycatSettingsPanel();
            montoyaApi.userInterface().registerSuiteTab("Copycat", settingsPanel.uiComponent());
        }

        montoyaApi.logging().logToOutput("Copycat extension loaded successfully!");
        montoyaApi.logging().logToOutput("Keyboard shortcut: Ctrl+Shift+C to copy request/response");
    }

    private class CopycatContextMenuProvider implements ContextMenuItemsProvider {
        @Override
        public List<Component> provideMenuItems(ContextMenuEvent event) {
            List<Component> menuItems = new ArrayList<>();

            if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER, ToolType.INTRUDER, ToolType.TARGET)) {
                JMenuItem copyRequestItem = new JMenuItem("Copy Request (Filtered)");
                copyRequestItem.addActionListener(new CopyRequestAction(event));
                menuItems.add(copyRequestItem);

                JMenuItem copyResponseItem = new JMenuItem("Copy Response (Filtered)");
                copyResponseItem.addActionListener(new CopyResponseAction(event));
                menuItems.add(copyResponseItem);

                // Removed configure menu item since settings are now in the tab
            }

            return menuItems;
        }
    }

    private class CopyRequestAction implements ActionListener {
        private final ContextMenuEvent event;

        public CopyRequestAction(ContextMenuEvent event) {
            this.event = event;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new Thread(() -> { // Run in background thread
                HttpRequest request = null;

                // Try to get request from selected items first (for list view)
                List<HttpRequestResponse> selectedItems = event.selectedRequestResponses();
                if (!selectedItems.isEmpty()) {
                    request = selectedItems.get(0).request();
                }
                // If no selected items, try to get from message editor
                else if (event.messageEditorRequestResponse().isPresent()) {
                    request = event.messageEditorRequestResponse().get().requestResponse().request();
                }

                if (request != null) {
                    String filteredRequest = filterHeaders(request.toString(), true);
                    SwingUtilities.invokeLater(() -> { // Update UI on EDT
                        copyToClipboard(filteredRequest);
                        api.logging().logToOutput("Request copied to clipboard (headers filtered)");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        api.logging().logToOutput("No request available to copy");
                    });
                }
            }).start();
        }
    }

    private class CopyResponseAction implements ActionListener {
        private final ContextMenuEvent event;

        public CopyResponseAction(ContextMenuEvent event) {
            this.event = event;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new Thread(() -> { // Run in background thread
                HttpResponse response = null;

                // Try to get response from selected items first (for list view)
                List<HttpRequestResponse> selectedItems = event.selectedRequestResponses();
                if (!selectedItems.isEmpty() && selectedItems.get(0).response() != null) {
                    response = selectedItems.get(0).response();
                }
                // If no selected items, try to get from message editor
                else if (event.messageEditorRequestResponse().isPresent() &&
                        event.messageEditorRequestResponse().get().requestResponse().response() != null) {
                    response = event.messageEditorRequestResponse().get().requestResponse().response();
                }

                if (response != null) {
                    String filteredResponse = filterHeaders(response.toString(), false);
                    SwingUtilities.invokeLater(() -> { // Update UI on EDT
                        copyToClipboard(filteredResponse);
                        api.logging().logToOutput("Response copied to clipboard (headers filtered)");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        api.logging().logToOutput("No response available to copy");
                    });
                }
            }).start();
        }
    }

    // Removed ConfigureHeadersAction class since settings are now in the tab

    private String filterHeaders(String message, boolean isRequest) {
        StringBuilder filtered = new StringBuilder();
        String[] lines = message.split("\\r?\\n");
        boolean inHeaders = true;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                inHeaders = false; // Headers end at first empty line
            }

            if (inHeaders) {
                // Check against precompiled patterns
                boolean exclude = false;
                for (Pattern pattern : precompiledPatterns) {
                    if (pattern.matcher(line).find()) {
                        exclude = true;
                        break;
                    }
                }

                if (!exclude) {
                    filtered.append(line).append("\n");
                }
            } else {
                // Body content
                filtered.append(line).append("\n");
            }
        }
        return filtered.toString();
    }

    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    private void registerKeyboardShortcuts() {
        // Create a global key listener for Ctrl+Shift+C
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                // Check for Ctrl+Shift+C combination
                if (e.getID() == KeyEvent.KEY_PRESSED &&
                        e.getKeyCode() == KeyEvent.VK_C &&
                        e.isControlDown() &&
                        e.isShiftDown()) {

                    // Handle the shortcut
                    handleQuickCopyShortcut();
                    return true; // Consume the event
                }
                return false; // Let other components handle the event
            }
        });
    }

    private void handleQuickCopyShortcut() {
        try {
            // Try to get current selection from various Burp tools
            // First, try to get from proxy history (most recent item)
            List<ProxyHttpRequestResponse> proxyHistory = api.proxy().history();
            if (!proxyHistory.isEmpty()) {
                ProxyHttpRequestResponse latest = proxyHistory.get(proxyHistory.size() - 1);

                // Check if we should copy request or response based on current focus
                // For now, we'll copy request by default, but user can press the shortcut again
                // for response
                if (latest.request() != null) {
                    String filteredRequest = filterHeaders(latest.request().toString(), true);
                    copyToClipboard(filteredRequest);
                    api.logging().logToOutput("Quick copy: Latest request copied to clipboard (Ctrl+Shift+C)");
                    return;
                }
            }

            // Try to get from site map if proxy history is empty
            try {
                // This is a fallback - we'll show a helpful message
                api.logging().logToOutput(
                        "Quick copy: No recent proxy traffic found. Use right-click context menu for specific requests.");
            } catch (Exception siteMapEx) {
                api.logging().logToOutput("Quick copy: No request/response available to copy");
            }

        } catch (Exception e) {
            api.logging().logToError("Error in quick copy shortcut: " + e.getMessage());
        }
    }

    private class CopycatSettingsPanel implements SettingsPanel {
        private JPanel mainPanel;
        private JProgressBar progressBar;

        @Override
        public JComponent uiComponent() {
            if (mainPanel == null) {
                mainPanel = createSettingsPanel();
            }
            return mainPanel;
        }

        @Override
        public Set<String> keywords() {
            return Set.of("copycat", "headers", "filter", "exclude", "copy", "request", "response");
        }

        private JPanel createSettingsPanel() {
            mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Title
            JLabel titleLabel = new JLabel("Copycat - Settings");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            // Settings content
            JPanel settingsPanel = new JPanel(new BorderLayout());
            settingsPanel.setBorder(BorderFactory.createTitledBorder("Excluded Headers Configuration"));

            // Description
            JTextArea descriptionArea = new JTextArea(
                    "Configure which headers to exclude when copying HTTP requests and responses.\n" +
                            "Header patterns listed below will be filtered out from the copied content.\n" +
                            "Supports regular expressions (regex) for flexible pattern matching.");
            descriptionArea.setEditable(false);
            descriptionArea.setOpaque(false);
            descriptionArea.setWrapStyleWord(true);
            descriptionArea.setLineWrap(true);
            descriptionArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
            settingsPanel.add(descriptionArea, BorderLayout.NORTH);

            // Header pattern list
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String pattern : excludedHeaderPatterns) {
                listModel.addElement(pattern);
            }
            JList<String> headerList = new JList<>(listModel);
            headerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            JScrollPane scrollPane = new JScrollPane(headerList);
            scrollPane.setPreferredSize(new Dimension(300, 200));
            settingsPanel.add(scrollPane, BorderLayout.CENTER);

            // Control panel
            JPanel controlPanel = new JPanel(new BorderLayout());

            // Input panel
            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JTextField newPatternField = new JTextField(20);
            JButton addButton = new JButton("Add Pattern");

            addButton.addActionListener(e -> {
                String newPattern = newPatternField.getText().trim();
                if (!newPattern.isEmpty() && !excludedHeaderPatterns.contains(newPattern)) {
                    progressBar.setVisible(true);
                    new Thread(() -> {
                        try {
                            Pattern.compile(newPattern);
                            SwingUtilities.invokeLater(() -> {
                                excludedHeaderPatterns.add(newPattern);
                                listModel.addElement(newPattern);
                                newPatternField.setText("");
                                api.logging().logToOutput("Added excluded header pattern: " + newPattern);
                                precompiledPatterns.add(Pattern.compile(newPattern, Pattern.CASE_INSENSITIVE));
                            });
                        } catch (PatternSyntaxException ex) {
                            SwingUtilities.invokeLater(() -> {
                                excludedHeaderPatterns.add(newPattern);
                                listModel.addElement(newPattern);
                                newPatternField.setText("");
                                api.logging().logToOutput("Added header pattern (treated as literal): " + newPattern
                                        + " - Invalid regex: " + ex.getMessage());
                                precompiledPatterns
                                        .add(Pattern.compile(Pattern.quote(newPattern), Pattern.CASE_INSENSITIVE));
                            });
                        } finally {
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setVisible(false);
                            });
                        }
                    }).start();
                }
            });

            // Allow Enter key to add pattern
            newPatternField.addActionListener(e -> addButton.doClick());

            inputPanel.add(new JLabel("Header pattern (regex):"));
            inputPanel.add(newPatternField);
            inputPanel.add(addButton);

            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton removeButton = new JButton("Remove Selected");
            JButton resetButton = new JButton("Reset to Defaults");

            removeButton.addActionListener(e -> {
                List<String> selected = headerList.getSelectedValuesList();
                for (String pattern : selected) {
                    excludedHeaderPatterns.remove(pattern);
                    listModel.removeElement(pattern);
                    api.logging().logToOutput("Removed excluded header pattern: " + pattern);
                }
            });

            resetButton.addActionListener(e -> {
                excludedHeaderPatterns.clear();
                excludedHeaderPatterns.addAll(Arrays.asList(
                        "content-length", "transfer-encoding", "connection",
                        "host", "accept-encoding", "user-agent"));
                listModel.clear();
                for (String pattern : excludedHeaderPatterns) {
                    listModel.addElement(pattern);
                }
                api.logging().logToOutput("Reset excluded header patterns to defaults");
            });

            buttonPanel.add(removeButton);
            buttonPanel.add(resetButton);

            controlPanel.add(inputPanel, BorderLayout.NORTH);
            controlPanel.add(buttonPanel, BorderLayout.SOUTH);

            // Add progress bar for pattern validation
            progressBar = new JProgressBar();
            progressBar.setVisible(false);
            controlPanel.add(progressBar, BorderLayout.SOUTH);

            settingsPanel.add(controlPanel, BorderLayout.SOUTH);
            mainPanel.add(settingsPanel, BorderLayout.CENTER);

            // Usage instructions
            JPanel instructionsPanel = new JPanel(new BorderLayout());
            instructionsPanel.setBorder(BorderFactory.createTitledBorder("Usage Instructions"));

            JTextArea instructionsArea = new JTextArea(
                    "How to use Copycat:\n" +
                            "1. Right-click on any HTTP request/response in Proxy, Repeater, Intruder, or Target\n" +
                            "   - Works in both list view and message editor tabs\n" +
                            "2. Select 'Copy Request (Filtered)' or 'Copy Response (Filtered)'\n" +
                            "3. Use keyboard shortcut Ctrl+Shift+C to quickly copy the latest request from Proxy\n" +
                            "4. The content will be copied to clipboard with excluded headers removed\n" +
                            "5. Use this settings panel to customize header exclusion patterns\n" +
                            "\nKeyboard Shortcut:\n" +
                            "• Ctrl+Shift+C - Quick copy latest request from Proxy history\n" +
                            "\nRegex Pattern Examples:\n" +
                            "• 'content-.*' - matches content-length, content-type, etc.\n" +
                            "• 'x-.*' - matches all X- headers\n" +
                            "• 'authorization' - exact match (literal string)\n" +
                            "• '(?i)COOKIE' - case-insensitive match for 'cookie'");
            instructionsArea.setEditable(false);
            instructionsArea.setOpaque(false);
            instructionsArea.setWrapStyleWord(true);
            instructionsArea.setLineWrap(true);
            instructionsArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            instructionsPanel.add(instructionsArea, BorderLayout.CENTER);

            mainPanel.add(instructionsPanel, BorderLayout.SOUTH);

            return mainPanel;
        }
    }
}