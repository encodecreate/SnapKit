package snap.view;

/**
 * A Menu is a MenuItem that pops up a dropdown menu.
 */
public class Menu extends MenuItem {

    // The menu items for this menu
    private MenuItem[] _menuItems;

    // Whether this menu's overlay dropdown is visible
    private boolean _popupShowing;

    /** Creates a new empty Menu. */
    public Menu()  { super(); }

    /** Sets the menu items. */
    public void setMenuItems(MenuItem[] menuItems)
    {
        _menuItems = menuItems;
    }

    /** Returns the menu items. */
    public MenuItem[] getMenuItems()  { return _menuItems; }

    /** Adds a menu item. */
    public void addItem(MenuItem item)
    {
        if (_menuItems == null) _menuItems = new MenuItem[0];
        MenuItem[] newArr = new MenuItem[_menuItems.length + 1];
        System.arraycopy(_menuItems, 0, newArr, 0, _menuItems.length);
        newArr[_menuItems.length] = item;
        _menuItems = newArr;
    }

    /** Shows the menu at the given (x, y) coordinates relative to the anchor. */
    public void showMenuAtXY(View anchor, double x, double y)
    {
        showMenuDropdown(anchor, x, y);
    }

    /** Shows the menu dropdown using the overlay pane. */
    public void showMenuDropdown(View anchor, double x, double y)
    {
        RootView rootView = anchor.getRootView();
        ParentView overlay = rootView.getMenuOverlay();

        // Remove previous dropdowns
        overlay.removeChildren();

        // Create dropdown content
        ColView menuDropdown = new ColView();
        if (_menuItems != null) {
            for (MenuItem item : _menuItems) {
                menuDropdown.addChild(createMenuItemView(item));
            }
        }
        menuDropdown.setMinWidth(125);
        menuDropdown.setFillWidth(true);
        menuDropdown.setPadding(4, 1, 4, 1);

        // Set position of dropdown
        menuDropdown.setXY(x, y);

        // Add to overlay and show overlay
        overlay.addChild(menuDropdown);
        overlay.setPickable(true);   // Overlay intercepts mouse events
        overlay.setVisible(true);
        _popupShowing = true;

        // Add event handler to hide dropdown when clicking outside
        overlay.addEventHandler(e -> {
            hide();
        }, ViewEvent.Type.MousePress);
    }

    /** Creates a view for a MenuItem (customize as needed). */
    protected ButtonBase createMenuItemView(MenuItem item)
    {
        ButtonBase btn = new ButtonBase();
        btn.setText(item.getText());
        // If you have icons or more, set them here.
        // Set up event handling for menu item selection:
        btn.setOnAction(e -> {
            // Call item action, then hide the menu
            item.fireActionEvent();
            hide();
        });
        return btn;
    }

    /** Hides the menu dropdown. */
    public void hide()
    {
        RootView rootView = getRootView();
        if (rootView == null) return;
        ParentView overlay = rootView.getMenuOverlay();
        overlay.removeChildren();
        overlay.setVisible(false);
        overlay.setPickable(false);
        _popupShowing = false;
    }

    /** Returns whether the menu popup is showing. */
    public boolean isPopupShowing()  { return _popupShowing; }

    /** Hide any child popups (for compatibility). */
    public void hideChildPopupWindows()  { hide(); }

    /** Fires the action event for a menu item (for compatibility). */
    public void itemFiredActionEvent()  { /* Implement as needed */ }

    /** Returns the popup for this menu (no longer supported, returns null). */
    public Object getPopup() { return null; }
}