package snap.view;

import snap.gfx.*;
import snap.util.*;

/**
 * Root view of the SnapKit window. Extended to support an overlay for menus.
 */
public class RootView extends ParentView {

    // The content view (main UI content)
    private View _content;

    // The overlay pane for menu dropdowns
    private ParentView _menuOverlay;

    /**
     * Constructor.
     */
    public RootView()
    {
        super();

        // Create and configure the menu overlay
        _menuOverlay = new ParentView();
        _menuOverlay.setPickable(false); // Don't intercept mouse events by default
        _menuOverlay.setVisible(false);  // Hidden until needed
        addChild(_menuOverlay);
    }

    /**
     * Returns the menu overlay pane.
     */
    public ParentView getMenuOverlay()
    {
        return _menuOverlay;
    }

    /**
     * Sets the content for this RootView.
     */
    public void setContent(View aView)
    {
        if (_content != null) removeChild(_content);
        _content = aView;
        if (_content != null) addChild(_content, 0); // Add below overlay
    }

    /**
     * Returns the content for this RootView.
     */
    public View getContent()  { return _content; }

    // ... (other RootView code as needed) ...
}