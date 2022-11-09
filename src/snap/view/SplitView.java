/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.view;
import java.util.*;
import snap.gfx.*;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.util.*;

/**
 * A View subclass to show children with user adjustable divider.
 */
public class SplitView extends ParentView implements ViewHost {

    // The list of items
    private List <View>  _items = new ArrayList<>();
    
    // The list of dividers
    private List <Divider>  _divs = new ArrayList<>();
    
    // The spacing between items (really the default span of the dividers)
    private double _divSpan = DEFAULT_DIVIDER_SPAN;
    
    // The default divider
    private Divider  _divider;
    
    // The divider currently being dragged (and the offset from center of drag start)
    private Divider  _dragDiv;
    private double  _dragOff;

    // A listener to watch for when item.Visible changes
    private PropChangeListener _visLsnr = pc -> itemVisibleChanged(pc);

    // Constants for properties
    public static final String DividerSpan_Prop = "DividerSpan";
    
    // Constants for internal use
    private static final Border SPLIT_VIEW_BORDER = Border.createLineBorder(Color.LIGHTGRAY,1);
    private static final int DEFAULT_DIVIDER_SPAN = 8;

    /**
     * Creates a new SplitView.
     */
    public SplitView()
    {
        setBorder(SPLIT_VIEW_BORDER);
        setClipToBounds(true);
        addEventFilter(e -> processDividerEvent(e), MouseMove, MousePress, MouseDrag, MouseRelease);
    }

    /**
     * Returns the number of items.
     */
    public int getItemCount()  { return _items.size(); }

    /**
     * Returns the individual item at given index.
     */
    public View getItem(int anIndex)  { return _items.get(anIndex); }

    /**
     * Override to make sure dividers are in place.
     */
    public void addItem(View aView)  { addItem(aView, getItemCount()); }

    /**
     * Returns the SplitView items.
     */
    public List <View> getItems()  { return _items; }

    /**
     * Override to make sure dividers are in place.
     */
    public void addItem(View aView, int anIndex)
    {
        // Add View item
        _items.add(anIndex, aView);

        // If more than one item, add divider
        if (getItemCount() > 1) {
            Divider div = createDivider();
            addDivider(div, anIndex > 0 ? (anIndex - 1) : 0);
            addChild(div, anIndex > 0 ? (anIndex * 2 - 1) : 0);

            // See if divider should be not-visible
            boolean vis = aView.isVisible();
            if (anIndex == 1)
                vis &= getItem(0).isVisible();
            div.setVisible(vis);
        }

        // Add view as child
        addChild(aView, anIndex * 2);
        aView.addPropChangeListener(_visLsnr, Visible_Prop);
    }

    /**
     * Override to remove unused dividers.
     */
    public View removeItem(int anIndex)
    {
        // Remove item and child and listener
        View view = _items.remove(anIndex);
        removeChild(view);
        view.removePropChangeListener(_visLsnr, Visible_Prop);

        // If at least one item left, remove extra divider
        if (getItemCount() > 0)
            removeDivider(anIndex > 0 ? (anIndex - 1) : 0);
        return view;
    }

    /**
     * Override to remove unused dividers.
     */
    public int removeItem(View aView)
    {
        int index = indexOfItem(aView);
        if (index >= 0)
            removeItem(index);
        return index;
    }

    /**
     * Sets the item at index.
     */
    public void setItem(View aView, int anIndex)
    {
        View old = anIndex < getItemCount() ? _items.get(anIndex) : null;
        int index = old != null ? removeItem(old) : -1;
        addItem(aView, index >= 0 ? index : getItemCount());
    }

    /**
     * Sets the splitview items to given views
     */
    public void setItems(View ... theViews)
    {
        removeItems();
        for (View view : theViews)
            addItem(view);
    }

    /**
     * Sets the splitview items to given views
     */
    public void removeItems()
    {
        for (View view : getItems().toArray(new View[0]))
            removeItem(view);
    }

    /**
     * Returns the index of given item.
     */
    public int indexOfItem(View anItem)  { return ListUtils.indexOfId(_items, anItem); }

    /**
     * Adds a child with animation.
     */
    public void addItemWithAnim(View aView, double aSize)
    {
        addItemWithAnim(aView, aSize, getItemCount());
    }

    /**
     * Adds a item with animation.
     */
    public void addItemWithAnim(View aView, double aSize, int anIndex)
    {
        // Add view as item
        addItem(aView, anIndex);

        // Get new Divider for view
        Divider div = anIndex == 0 ? getDivider(0) : getDivider(anIndex - 1);

        // If first view, configure anim for given size as Location
        if (anIndex == 0) {
            div.setLocation(0);
            div.getAnimCleared(500).setValue(Divider.Location_Prop, 1d, aSize).play();
        }

        // If successive view, configure anim for given size as Remainder
        else {
            div.setRemainder(1);
            div.getAnimCleared(500).setValue(Divider.Remainder_Prop, 1d, aSize).play();
        }
    }

    /**
     * Removes a item with animation.
     */
    public void removeItemWithAnim(View aView)
    {
        // Get index, divider and Location/Remainder for given view
        int index = indexOfItem(aView);
        Divider div = index == 0 ? getDivider(0) : getDivider(index - 1);
        double size = isVertical() ? aView.getHeight() : aView.getWidth();

        // If first item, set Location animated
        if (index == 0) {
            div.setLocation(size);
            ViewAnim anim = div.getAnim(0).clear();
            anim.getAnim(500).setValue(Divider.Location_Prop, size, 1d);
            anim.setOnFinish(() -> removeItem(aView)).needsFinish().play();
        }

        // If not first item, set Remainder animated
        else {
            div.setRemainder(size);
            ViewAnim anim = div.getAnim(0).clear();
            anim.getAnim(500).setValue(Divider.Remainder_Prop, size, 1d);
            anim.setOnFinish(() -> removeItem(aView)).needsFinish().play();
        }
    }

    /**
     * Sets a child visible with animation.
     */
    public void setItemVisibleWithAnim(View aView, boolean aValue)
    {
        // If already set, just return
        if (aValue == aView.isVisible()) return;

        // Get index, divider and size
        int index = indexOfItem(aView), time = 500;
        Divider div = index == 0 ? getDivider(0) : getDivider(index - 1);
        double size = isVertical() ? aView.getHeight() : aView.getWidth();

        // Clear running anims
        aView.getAnimCleared(0);
        div.getAnimCleared(0);

        // Handle show item
        if (aValue) {

            // If first item, set Location
            double dsize = div.getSpan();
            if (index == 0) {
                div.setLocation(0);
                div.getAnim(time).setValue(Divider.Location_Prop, dsize, size).play();
            }

            // If not first item, set Remainder
            else {
                div.setRemainder(1);
                div.getAnim(time).setValue(Divider.Remainder_Prop, dsize, size).play();
            }

            // Show view and divider
            aView.setVisible(true);
            aView.setOpacity(0);
            aView.getAnim(time).setOpacity(1).play();
            div.setOpacity(0);
            div.getAnim(time).setOpacity(1).play();
        }

        // Handle hide item
        else {

            // If first item, set location
            if (index == 0) {
                div.setLocation(size);
                div.getAnim(time).setValue(Divider.Location_Prop, size, 1d).play();
            }

            // If non-first item, set remainder
            else {
                div.setRemainder(size);
                div.getAnim(time).setValue(Divider.Remainder_Prop, size, 1d).play();
            }

            // Clear
            aView.setOpacity(1);
            div.setOpacity(1);
            div.getAnim(time).setOpacity(0).play();

            // Configure anim
            aView.getAnim(time).setOpacity(0).setOnFinish(() -> setItemVisibleWithAnimDone(aView, div, size)).play();
        }
    }

    /**
     * Called when setItemVisibleWithAnim is done.
     */
    private void setItemVisibleWithAnimDone(View aView,Divider aDiv, double size)
    {
        aView.setVisible(false);
        aView.setOpacity(1);
        aDiv.setOpacity(1);
        if (isVertical())
            aView.setHeight(size);
        else aView.setWidth(size);
    }

    /**
     * Called when an item changes the value of visible property.
     */
    private void itemVisibleChanged(PropChange aPC)
    {
        // If no dividers, just return
        if (getItemCount() < 2) return;

        // Get whether divider should be visible and fix
        View view = (View) aPC.getSource();
        int viewIndex = getItems().indexOf(view);
        Divider div = getDivider(viewIndex > 0 ? viewIndex - 1 : 0);
        boolean divVisible = view.isVisible();
        if (viewIndex == 1)
            divVisible &= getItem(0).isVisible();
        div.setVisible(divVisible);
    }

    /**
     * Returns the default divider.
     */
    public Divider getDivider()
    {
        // If already set, just return
        if (_divider != null) return _divider;

        // Create and return
        Divider div = new Divider();
        div.setVertical(!isVertical());
        div.setBorder(Divider.DIVIDER_BORDER);
        div.addPropChangeListener(pc -> dividerPropChange(pc), Fill_Prop, Border_Prop);
        return _divider = div;
    }

    /**
     * Creates a new divider.
     */
    protected Divider createDivider()
    {
        // Create/config new divider from prototype
        Divider dividerPrototype = getDivider();
        Divider newDivider = new Divider();
        newDivider.setVertical(!isVertical());
        newDivider.setFill(dividerPrototype.getFill());
        newDivider.setBorder(dividerPrototype.getBorder());
        newDivider.setReach(dividerPrototype.getReach());
        newDivider.setPrefSpan(getDividerSpan());

        // Return
        return newDivider;
    }

    /**
     * Returns the number of dividers.
     */
    public int getDividerCount()  { return _divs.size(); }

    /**
     * Returns the individual divider at given index.
     */
    public Divider getDivider(int anIndex)  { return _divs.get(anIndex); }

    /**
     * Adds a new divider.
     */
    protected void addDivider(Divider aDiv, int anIndex)  { _divs.add(anIndex, aDiv); }

    /**
     * Removes a divider.
     */
    protected Divider removeDivider(int anIndex)
    {
        Divider div = _divs.remove(anIndex);
        removeChild(div);
        return div;
    }

    /**
     * Returns the dividers.
     */
    public Divider[] getDividers()  { return _divs.toArray(new Divider[0]); }

    /**
     * Returns the divider at given point.
     */
    public Divider getDividerAt(double aX, double aY)
    {
        // Handle vertical
        if (isVertical()) {
            for (Divider div : _divs) {
                if (!div.isVisible()) continue;
                double min = div.getY() - div.getReach();
                double max = div.getMaxY() + div.getReach();
                if (aY >= min && aY <= max)
                    return div;
            }
        }

        // Handle horizontal
        else {
            for (Divider div : _divs) {
                if (!div.isVisible()) continue;
                double min = div.getX() - div.getReach();
                double max = div.getMaxX() + div.getReach();
                if (aX >= min && aX <= max)
                    return div;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns the default size of the dividers.
     */
    public double getDividerSpan()  { return _divSpan; }

    /**
     * Sets the default size of the dividers.
     */
    public void setDividerSpan(double aValue)
    {
        if (aValue == _divSpan) return;
        for (Divider div : _divs)
            div.setPrefSpan(aValue);
        firePropChange(DividerSpan_Prop, _divSpan, _divSpan = aValue);
    }

    /**
     * Called when prototype divider has prop change to propogate to active dividers.
     */
    private void dividerPropChange(PropChange aPC)
    {
        // Get property name
        String propName = aPC.getPropName();

        // Handle Fill, Border
        if (propName == Fill_Prop)
            for (Divider div : _divs)
                div.setFill(_divider.getFill());
        else if (propName == Border_Prop)
            for (Divider div : _divs)
                div.setBorder(_divider.getBorder());
    }

    /**
     * Calculates the preferred width.
     */
    protected double getPrefWidthImpl(double aH)
    {
        ParentViewProxy<?> viewProxy = getViewProxy();
        return viewProxy.getPrefWidth(aH);
    }

    /**
     * Calculates the preferred height.
     */
    protected double getPrefHeightImpl(double aW)
    {
        ParentViewProxy<?> viewProxy = getViewProxy();
        return viewProxy.getPrefHeight(aW);
    }

    /**
     * Override to layout children.
     */
    protected void layoutImpl()
    {
        ParentViewProxy<?> viewProxy = getViewProxy();
        viewProxy.layoutView();

        // After any layout, all pref sizes should be set
        makeSurePrefSizesAreSet();
    }

    /**
     * After any layout, all pref sizes should be set
     */
    private void makeSurePrefSizesAreSet()
    {
        List<View> items = getItems();

        // Handle Vertical: Iterate over items and make sure PrefHeight is set
        if (isVertical()) {
            for (View item : items)
                if (!item.isPrefHeightSet())
                    item.setPrefHeight(item.getHeight());
        }

        // Handle Vertical: Iterate over items and make sure PrefWidth is set
        else {
            for (View item : items)
                if (!item.isPrefWidthSet())
                    item.setPrefWidth(item.getWidth());
        }
    }

    /**
     * Override to return RowViewProxy or ColViewProxy.
     */
    @Override
    protected ParentViewProxy<?> getViewProxy()
    {
        ParentViewProxy<?> viewProxy = isHorizontal() ? new RowViewProxy<>(this) :
            new ColViewProxy<>(this);
        viewProxy.setFillWidth(true);
        viewProxy.setFillHeight(true);
        return viewProxy;
    }

    /**
     * Handle MouseDrag event: Calculate and set new location.
     */
    protected void processDividerEvent(ViewEvent anEvent)
    {
        // Handle MouseMove: If over divider, update cursor
        if (anEvent.isMouseMove()) {
            Divider div = getDividerAt(anEvent.getX(), anEvent.getY());
            if (div != null) {
                WindowView win = getWindow();
                if (win != null)
                    win.setActiveCursor(div.getCursor());
            }
        }

        // Handle MousePress: Check for divider hit
        else if (anEvent.isMousePress()) {

            // Get divider at mouse
            _dragDiv = getDividerAt(anEvent.getX(), anEvent.getY());
            if (_dragDiv == null)
                return;

            // Set divider drag offset
            _dragOff = isVertical() ? _dragDiv.getY() - anEvent.getY() : _dragDiv.getX() - anEvent.getX();
            anEvent.consume();
        }

        // Handle MouseDrag: Calculate new location and set
        else if (anEvent.isMouseDrag() && _dragDiv != null) {
            View peer0 = _dragDiv.getViewBefore();
            double loc = _dragDiv.isVertical() ? (anEvent.getX() - peer0.getX()) : (anEvent.getY() - peer0.getY());
            _dragDiv.setLocation(loc + _dragOff);
            anEvent.consume();
        }

        // Handle MouseRelease: Clear DragDiv
        else if (anEvent.isMouseRelease() && _dragDiv != null) {
            _dragDiv = null;
            anEvent.consume();
        }
    }

    /**
     * Returns the default border.
     */
    public Border getDefaultBorder()  { return SPLIT_VIEW_BORDER; }

    /**
     * Override to forward to dividers.
     */
    public void setVertical(boolean aValue)
    {
        if (aValue == isVertical()) return;
        super.setVertical(aValue);
        if (_divider != null)
            _divider.setVertical(!aValue);
        for (Divider div : _divs)
            div.setVertical(!aValue);
    }

    /**
     * ViewHost method.
     */
    @Override
    public int getGuestCount()  { return getItemCount(); }

    /**
     * ViewHost method.
     */
    @Override
    public View getGuest(int anIndex)  { return getItem(anIndex); }

    /**
     * ViewHost method.
     */
    @Override
    public void addGuest(View aChild, int anIndex)  { addItem(aChild, anIndex); }

    /**
     * ViewHost method.
     */
    @Override
    public View removeGuest(int anIndex)  { return removeItem(anIndex); }

    /**
     * XML Archival of basic view.
     */
    protected XMLElement toXMLView(XMLArchiver anArchiver)
    {
        // Archive basic view attributes
        XMLElement e = super.toXMLView(anArchiver);

        // Archive DividerSpan
        if (getDividerSpan() != DEFAULT_DIVIDER_SPAN)
            e.add(DividerSpan_Prop, getDividerSpan());

        // Return
        return e;
    }

    /**
     * XML unarchival of basic view.
     */
    protected void fromXMLView(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Unarchive basic view attributes
        super.fromXMLView(anArchiver, anElement);

        // Unarchive DividerSpan
        if(anElement.hasAttribute(DividerSpan_Prop))
            setDividerSpan(anElement.getAttributeFloatValue(DividerSpan_Prop));
    }

    /**
     * XML archival deep.
     */
    public void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Archive items
        for (View item : getItems()) {
            XMLElement cxml = anArchiver.toXML(item, this);
            anElement.add(cxml);
        }
    }

    /**
     * XML unarchival for shape children.
     */
    protected void fromXMLChildren(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Iterate over child elements and unarchive as child nodes
        for (int i = 0, iMax = anElement.size(); i < iMax; i++) {
            XMLElement childXML = anElement.get(i);
            Class<?> cls = anArchiver.getClass(childXML.getName());
            if (cls != null && View.class.isAssignableFrom(cls)) {
                View view = (View) anArchiver.fromXML(childXML, this);
                addItem(view);
            }
        }
    }

    /**
     * Replaces the given view with a SplitView.
     */
    public static SplitView makeSplitView(View aView)
    {
        // Create SplitView to match given view
        SplitView splitView = new SplitView();
        splitView.setVertical(aView.isVertical());
        splitView.setLeanX(aView.getLeanX());
        splitView.setLeanY(aView.getLeanY());
        splitView.setGrowWidth(aView.isGrowWidth());
        splitView.setGrowHeight(aView.isGrowHeight());

        // Handle ViewHost
        if (aView instanceof ViewHost) {
            ViewHost host = (ViewHost) aView;
            splitView.setItems(host.getGuests());
        }

        // Replace given View with new SplitView and return SplitView
        if (aView.getParent() != null)
           ViewUtils.replaceView(aView, splitView);

        // Return
        return splitView;
    }
}