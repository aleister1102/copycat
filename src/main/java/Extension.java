import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Extension implements BurpExtension {
    private MontoyaApi api;
    private Set<String> excludedHeaders;
    
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.api = montoyaApi;
        montoyaApi.extension().setName("HTTP Copier");
        
        // Initialize default excluded headers
        excludedHeaders = new HashSet<>(Arrays.asList(
            "content-length", "transfer-encoding", "connection", 
            "host", "accept-encoding", "user-agent"
        ));
        
        // Register context menu provider
        montoyaApi.userInterface().registerContextMenuItemsProvider(new HttpCopierContextMenuProvider());
        
        // Register settings tab
        montoyaApi.userInterface().registerSuiteTab("HTTP Copier", createSettingsPanel());
        
        montoyaApi.logging().logToOutput("HTTP Copier extension loaded successfully!");
    }
    
    private class HttpCopierContextMenuProvider implements ContextMenuItemsProvider {
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
            List<HttpRequestResponse> selectedItems = event.selectedRequestResponses();
            if (!selectedItems.isEmpty()) {
                HttpRequest request = selectedItems.get(0).request();
                String filteredRequest = filterHeaders(request.toString(), true);
                copyToClipboard(filteredRequest);
                api.logging().logToOutput("Request copied to clipboard (headers filtered)");
            }
        }
    }
    
    private class CopyResponseAction implements ActionListener {
        private final ContextMenuEvent event;
        
        public CopyResponseAction(ContextMenuEvent event) {
            this.event = event;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            List<HttpRequestResponse> selectedItems = event.selectedRequestResponses();
            if (!selectedItems.isEmpty() && selectedItems.get(0).response() != null) {
                HttpResponse response = selectedItems.get(0).response();
                String filteredResponse = filterHeaders(response.toString(), false);
                copyToClipboard(filteredResponse);
                api.logging().logToOutput("Response copied to clipboard (headers filtered)");
            } else {
                api.logging().logToOutput("No response available to copy");
            }
        }
    }
    
    // Removed ConfigureHeadersAction class since settings are now in the tab
    
    private String filterHeaders(String httpMessage, boolean isRequest) {
        String[] lines = httpMessage.split("\r?\n");
        StringBuilder filtered = new StringBuilder();
        boolean inHeaders = true;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // First line is always included (request/response line)
            if (i == 0) {
                filtered.append(line).append("\r\n");
                continue;
            }
            
            // Empty line marks end of headers
            if (line.trim().isEmpty()) {
                inHeaders = false;
                filtered.append(line).append("\r\n");
                continue;
            }
            
            if (inHeaders) {
                // Check if this header should be excluded
                String headerName = line.split(":")[0].trim().toLowerCase();
                if (!excludedHeaders.contains(headerName)) {
                    filtered.append(line).append("\r\n");
                }
            } else {
                // Body content - include as is
                filtered.append(line).append("\r\n");
            }
        }
        
        return filtered.toString();
    }
    
    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }
    
    private Component createSettingsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("HTTP Copier Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Settings content
        JPanel settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Excluded Headers Configuration"));
        
        // Description
        JTextArea descriptionArea = new JTextArea(
            "Configure which headers to exclude when copying HTTP requests and responses.\n" +
            "Headers listed below will be filtered out from the copied content."
        );
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        settingsPanel.add(descriptionArea, BorderLayout.NORTH);
        
        // Header list
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String header : excludedHeaders) {
            listModel.addElement(header);
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
        JTextField newHeaderField = new JTextField(20);
        JButton addButton = new JButton("Add Header");
        
        addButton.addActionListener(e -> {
            String newHeader = newHeaderField.getText().trim().toLowerCase();
            if (!newHeader.isEmpty() && !excludedHeaders.contains(newHeader)) {
                excludedHeaders.add(newHeader);
                listModel.addElement(newHeader);
                newHeaderField.setText("");
                api.logging().logToOutput("Added excluded header: " + newHeader);
            }
        });
        
        // Allow Enter key to add header
        newHeaderField.addActionListener(e -> addButton.doClick());
        
        inputPanel.add(new JLabel("Header name:"));
        inputPanel.add(newHeaderField);
        inputPanel.add(addButton);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton removeButton = new JButton("Remove Selected");
        JButton resetButton = new JButton("Reset to Defaults");
        
        removeButton.addActionListener(e -> {
            List<String> selected = headerList.getSelectedValuesList();
            for (String header : selected) {
                excludedHeaders.remove(header);
                listModel.removeElement(header);
                api.logging().logToOutput("Removed excluded header: " + header);
            }
        });
        
        resetButton.addActionListener(e -> {
            excludedHeaders.clear();
            excludedHeaders.addAll(Arrays.asList(
                "content-length", "transfer-encoding", "connection", 
                "host", "accept-encoding", "user-agent"
            ));
            listModel.clear();
            for (String header : excludedHeaders) {
                listModel.addElement(header);
            }
            api.logging().logToOutput("Reset excluded headers to defaults");
        });
        
        buttonPanel.add(removeButton);
        buttonPanel.add(resetButton);
        
        controlPanel.add(inputPanel, BorderLayout.NORTH);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        settingsPanel.add(controlPanel, BorderLayout.SOUTH);
        mainPanel.add(settingsPanel, BorderLayout.CENTER);
        
        // Usage instructions
        JPanel instructionsPanel = new JPanel(new BorderLayout());
        instructionsPanel.setBorder(BorderFactory.createTitledBorder("Usage Instructions"));
        
        JTextArea instructionsArea = new JTextArea(
            "How to use HTTP Copier:\n" +
            "1. Right-click on any HTTP request/response in Proxy, Repeater, Intruder, or Target\n" +
            "2. Select 'Copy Request (Filtered)' or 'Copy Response (Filtered)'\n" +
            "3. The content will be copied to clipboard with excluded headers removed\n" +
            "4. Use this settings panel to customize which headers are excluded"
        );
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