package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.settings.SettingsPanel;

public class CopycatSettingsPanel implements SettingsPanel {
    private final MontoyaApi api;
    private final Set<String> excludedHeaderPatterns;
    private final Runnable recompilePatternsCallback;
    
    private JPanel mainPanel;
    private JProgressBar progressBar;

    public CopycatSettingsPanel(MontoyaApi api, Set<String> excludedHeaderPatterns, Runnable recompilePatternsCallback) {
        this.api = api;
        this.excludedHeaderPatterns = excludedHeaderPatterns;
        this.recompilePatternsCallback = recompilePatternsCallback;
    }

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

        mainPanel.add(createTitleLabel(), BorderLayout.NORTH);
        mainPanel.add(createHeaderSettingsPanel(), BorderLayout.CENTER);
        mainPanel.add(createInstructionsPanel(), BorderLayout.SOUTH);

        return mainPanel;
    }
    
    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("Copycat - Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        return titleLabel;
    }
    
    private JPanel createHeaderSettingsPanel() {
        JPanel settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Excluded Headers Configuration"));

        settingsPanel.add(createDescriptionArea(), BorderLayout.NORTH);
        settingsPanel.add(createHeaderListPanel(), BorderLayout.CENTER);
        settingsPanel.add(createControlPanel(), BorderLayout.SOUTH);
        
        return settingsPanel;
    }
    
    private JTextArea createDescriptionArea() {
        JTextArea descriptionArea = new JTextArea("""
                Configure which headers to exclude when copying HTTP requests and responses.
                Header patterns listed below will be filtered out from the copied content.
                Supports regular expressions (regex) for flexible pattern matching.""");
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        return descriptionArea;
    }
    
    private JScrollPane createHeaderListPanel() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        excludedHeaderPatterns.forEach(listModel::addElement);
        
        JList<String> headerList = new JList<>(listModel);
        headerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(headerList);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        return scrollPane;
    }
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        // Create components
        JTextField newPatternField = new JTextField(20);
        JButton addButton = new JButton("Add Pattern");
        JButton removeButton = new JButton("Remove Selected");  
        JButton resetButton = new JButton("Reset to Defaults");
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        
        // Setup event handlers
        setupControlHandlers(newPatternField, addButton, removeButton, resetButton);
        
        // Layout components
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Header pattern (regex):"));
        inputPanel.add(newPatternField);
        inputPanel.add(addButton);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(removeButton);
        buttonPanel.add(resetButton);
        
        controlPanel.add(inputPanel, BorderLayout.NORTH);
        controlPanel.add(buttonPanel, BorderLayout.CENTER);
        controlPanel.add(progressBar, BorderLayout.SOUTH);
        
        return controlPanel;
    }
    
    private void setupControlHandlers(JTextField newPatternField, JButton addButton, 
                                    JButton removeButton, JButton resetButton) {
        // Get the list model from the scroll pane
        JScrollPane scrollPane = (JScrollPane) ((JPanel) mainPanel.getComponent(1)).getComponent(1);
        JList<String> headerList = (JList<String>) scrollPane.getViewport().getView();
        DefaultListModel<String> listModel = (DefaultListModel<String>) headerList.getModel();
        
        addButton.addActionListener(e -> addHeaderPattern(newPatternField, listModel));
        newPatternField.addActionListener(e -> addButton.doClick());
        removeButton.addActionListener(e -> removeSelectedPatterns(headerList, listModel));
        resetButton.addActionListener(e -> resetToDefaults(listModel));
    }
    
    private void addHeaderPattern(JTextField field, DefaultListModel<String> listModel) {
        String newPattern = field.getText().trim();
        if (newPattern.isEmpty() || excludedHeaderPatterns.contains(newPattern)) return;
        
        progressBar.setVisible(true);
        new Thread(() -> {
            try {
                Pattern.compile(newPattern);
                SwingUtilities.invokeLater(() -> updatePatternList(newPattern, listModel, field, false));
            } catch (PatternSyntaxException ex) {
                SwingUtilities.invokeLater(() -> updatePatternList(newPattern, listModel, field, true));
            } finally {
                SwingUtilities.invokeLater(() -> progressBar.setVisible(false));
            }
        }).start();
    }
    
    private void updatePatternList(String pattern, DefaultListModel<String> listModel, 
                                 JTextField field, boolean isLiteral) {
        excludedHeaderPatterns.add(pattern);
        listModel.addElement(pattern);
        field.setText("");
        recompilePatternsCallback.run();
        
        String message = isLiteral 
            ? "Added header pattern (treated as literal): " + pattern
            : "Added excluded header pattern: " + pattern;
        api.logging().logToOutput(message);
    }
    
    private void removeSelectedPatterns(JList<String> headerList, DefaultListModel<String> listModel) {
        List<String> selected = headerList.getSelectedValuesList();
        for (String pattern : selected) {
            excludedHeaderPatterns.remove(pattern);
            listModel.removeElement(pattern);
            api.logging().logToOutput("Removed excluded header pattern: " + pattern);
        }
        recompilePatternsCallback.run();
    }
    
    private void resetToDefaults(DefaultListModel<String> listModel) {
        excludedHeaderPatterns.clear();
        excludedHeaderPatterns.addAll(Arrays.asList(
            "content-length", "transfer-encoding", "connection",
            "host", "accept-encoding", "user-agent", "sec-.*"
        ));
        listModel.clear();
        excludedHeaderPatterns.forEach(listModel::addElement);
        recompilePatternsCallback.run();
        api.logging().logToOutput("Reset excluded header patterns to defaults");
    }
    
    private JPanel createInstructionsPanel() {
        JPanel instructionsPanel = new JPanel(new BorderLayout());
        instructionsPanel.setBorder(BorderFactory.createTitledBorder("Usage Instructions"));

        JTextArea instructionsArea = new JTextArea("""
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
                • '(?i)COOKIE' - case-insensitive match for 'cookie'""");
        instructionsArea.setEditable(false);
        instructionsArea.setOpaque(false);
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setLineWrap(true);
        instructionsArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        instructionsPanel.add(instructionsArea, BorderLayout.CENTER);

        return instructionsPanel;
    }
}
