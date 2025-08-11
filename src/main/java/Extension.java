import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import ui.CopycatContextMenuProvider;
import ui.CopycatSettingsPanel;

public class Extension implements BurpExtension {
    private MontoyaApi api;
    private Set<String> excludedHeaderPatterns;
    private Set<Pattern> precompiledPatterns;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.api = montoyaApi;
        montoyaApi.extension().setName(CopycatConstants.EXTENSION_NAME);

        initializeExcludedHeaders();
        registerComponents(montoyaApi);

        montoyaApi.logging().logToOutput(CopycatConstants.EXTENSION_LOADED);
    }

    private void initializeExcludedHeaders() {
        excludedHeaderPatterns = new HashSet<>(Arrays.asList(CopycatConstants.DEFAULT_PATTERNS));
        recompilePatterns();
    }

    private void recompilePatterns() {
        if (precompiledPatterns == null) {
            precompiledPatterns = new HashSet<>();
        } else {
            precompiledPatterns.clear();
        }
        for (String pattern : excludedHeaderPatterns) {
            try {
                precompiledPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                precompiledPatterns.add(Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE));
            }
        }
    }

    private void registerComponents(MontoyaApi montoyaApi) {
        montoyaApi.userInterface().registerContextMenuItemsProvider(
                new CopycatContextMenuProvider(api, precompiledPatterns));

        try {
            montoyaApi.userInterface().registerSettingsPanel(
                    new CopycatSettingsPanel(api, excludedHeaderPatterns, this::recompilePatterns));
            montoyaApi.logging().logToOutput(CopycatConstants.SETTINGS_REGISTERED);
        } catch (Exception e) {
            montoyaApi.logging().logToOutput(CopycatConstants.SETTINGS_FALLBACK + e.getMessage());
            CopycatSettingsPanel settingsPanel = new CopycatSettingsPanel(api, excludedHeaderPatterns,
                    this::recompilePatterns);
            montoyaApi.userInterface().registerSuiteTab(CopycatConstants.TAB_NAME, settingsPanel.uiComponent());
        }
    }
}
