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

public class CopyBothAction implements ActionListener {
	private final ContextMenuEvent e;
	private final MontoyaApi api;
	private final Set<Pattern> p;

	public CopyBothAction(ContextMenuEvent e, MontoyaApi api, Set<Pattern> p) {
		this.e = e;
		this.api = api;
		this.p = p;
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		new Thread(() -> {
			String rq = getReq();
			String rs = getRes();
			String out = rq != null && rs != null ? rq + "\r\n\r\n" + rs : rq != null ? rq : rs;
			if (out != null) {
				String fOut = out;
				SwingUtilities.invokeLater(() -> {
					copy(fOut);
					api.logging().logToOutput("Request+Response copied to clipboard (headers filtered)");
				});
			} else {
				SwingUtilities.invokeLater(() -> api.logging().logToOutput("No request/response available to copy"));
			}
		}).start();
	}

	private String getReq() {
		HttpRequest r = extReq();
		if (r == null) return null;
		return filt(r.toString(), r.headers(), r.bodyToString());
	}

	private String getRes() {
		HttpResponse r = extRes();
		if (r == null) return null;
		return filt(r.toString(), r.headers(), r.bodyToString());
	}

	private String filt(String raw, List h, String b) {
		StringBuilder s = new StringBuilder();
		String first = raw.substring(0, raw.indexOf("\r\n"));
		s.append(first).append("\r\n");
		h.forEach(x -> {
			String n = x.toString().split(":")[0].trim();
			boolean ex = p.stream().anyMatch(q -> q.matcher(n).find());
			if (!ex) s.append(x.toString()).append("\r\n");
		});
		s.append("\r\n").append(b);
		return s.toString();
	}

	private HttpRequest extReq() {
		List<HttpRequestResponse> s = e.selectedRequestResponses();
		if (!s.isEmpty()) return s.get(0).request();
		return e.messageEditorRequestResponse().map(x -> x.requestResponse().request()).orElse(null);
	}

	private HttpResponse extRes() {
		List<HttpRequestResponse> s = e.selectedRequestResponses();
		if (!s.isEmpty() && s.get(0).response() != null) return s.get(0).response();
		return e.messageEditorRequestResponse().map(x -> x.requestResponse().response()).orElse(null);
	}

	private void copy(String t) {
		StringSelection sel = new StringSelection(t);
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		cb.setContents(sel, null);
	}
}


