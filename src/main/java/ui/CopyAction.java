package ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;

public class CopyAction implements ActionListener {
    private final ContextMenuEvent event;
    private final MontoyaApi api;
    private final Set<Pattern> precompiledPatterns;
    private final boolean isRequest;

    public CopyAction(ContextMenuEvent event, MontoyaApi api, Set<Pattern> precompiledPatterns, boolean isRequest) {
        this.event = event;
        this.api = api;
        this.precompiledPatterns = precompiledPatterns;
        this.isRequest = isRequest;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new Thread(() -> {
            String filtered = isRequest ? getFilteredRequest() : getFilteredResponse();
            if (filtered != null) {
                SwingUtilities.invokeLater(() -> {
                    copyToClipboard(filtered);
                    String type = isRequest ? "Request" : "Response";
                    api.logging().logToOutput(type + " copied to clipboard (headers filtered)");
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    String type = isRequest ? "request" : "response";
                    api.logging().logToOutput("No " + type + " available to copy");
                });
            }
        }).start();
    }
    
    private String getFilteredRequest() {
        HttpRequest request = extractHttpRequest();
        if (request == null) return null;
        return filterHttpMessage(request.toString(), request.headers(), request.bodyToString());
    }
    
    private String getFilteredResponse() {
        HttpResponse response = extractHttpResponse();
        if (response == null) return null;
        return filterHttpMessage(response.toString(), response.headers(), response.bodyToString());
    }
    
    private String filterHttpMessage(String rawMessage, List headers, String body) {
        StringBuilder filtered = new StringBuilder();
        
        // Append first line (request line or status line)
        String firstLine = rawMessage.substring(0, rawMessage.indexOf("\r\n"));
        filtered.append(firstLine).append("\r\n");
        
        // Filter and append headers
        headers.forEach(header -> {
            String headerName = header.toString().split(":")[0].trim();
            boolean exclude = precompiledPatterns.stream()
                    .anyMatch(pattern -> pattern.matcher(headerName).find());
            if (!exclude) {
                filtered.append(header.toString()).append("\r\n");
            }
        });
        
        // Append empty line separator and body
        filtered.append("\r\n").append(body);
        return filtered.toString();
    }
    
    private HttpRequest extractHttpRequest() {
        List<HttpRequestResponse> selected = event.selectedRequestResponses();
        if (!selected.isEmpty()) {
            return selected.get(0).request();
        }
        return event.messageEditorRequestResponse()
                .map(editor -> editor.requestResponse().request())
                .orElse(null);
    }
    
    private HttpResponse extractHttpResponse() {
        List<HttpRequestResponse> selected = event.selectedRequestResponses();
        if (!selected.isEmpty() && selected.get(0).response() != null) {
            return selected.get(0).response();
        }
        return event.messageEditorRequestResponse()
                .map(editor -> editor.requestResponse().response())
                .orElse(null);
    }
    
    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }
}
