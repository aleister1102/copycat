package ui;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JMenuItem;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

public class CopycatContextMenuProvider implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final Set<Pattern> precompiledPatterns;

    public CopycatContextMenuProvider(MontoyaApi api, Set<Pattern> precompiledPatterns) {
        this.api = api;
        this.precompiledPatterns = precompiledPatterns;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        if (!event.isFromTool(ToolType.PROXY, ToolType.REPEATER, ToolType.INTRUDER, ToolType.TARGET)) {
            return new ArrayList<>();
        }
        
        List<Component> menuItems = new ArrayList<>();
        menuItems.add(createMenuItem("Copy Request (Filtered)", new CopyAction(event, api, precompiledPatterns, true)));
        menuItems.add(createMenuItem("Copy Response (Filtered)", new CopyAction(event, api, precompiledPatterns, false)));
        if (event.isFromTool(ToolType.PROXY)) {
            menuItems.add(createMenuItem("Copy Request+Response (Filtered)", new CopyBothAction(event, api, precompiledPatterns)));
        }
        return menuItems;
    }
    
    private JMenuItem createMenuItem(String text, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(action);
        return item;
    }
}
