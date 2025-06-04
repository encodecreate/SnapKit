package snap.view;

import snap.gfx.*;
import snap.util.*;

/**
 * Root view of the SnapKit window. Extended to support an overlay for menus.
 */
public class RootView extends ParentView {

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
        _menuOverlay.setPickable(false);
        _menuOverlay.setVisible(false);
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
        // Remove all children except overlay
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChild(i);
            if (child != _menuOverlay)
                removeChild(child);
        }
        // Add new content as the first child (below overlay)
        if (aView != null)
            addChild(aView, 0);
    }

    /**
     * Returns the content for this RootView.
     * Backwards compatible: returns the first child that is not the overlay.
     */
    public View getContent()
    {
        for (int i = 0, n = getChildCount(); i < n; i++) {
            View child = getChild(i);
            if (child != _menuOverlay)
                return child;
        }
        return null;
    }
}