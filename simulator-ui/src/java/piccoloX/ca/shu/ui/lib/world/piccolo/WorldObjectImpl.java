package ca.shu.ui.lib.world.piccolo;

import java.awt.Paint;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import ca.shu.ui.lib.objects.activities.TransientMessage;
import ca.shu.ui.lib.util.Util;
import ca.shu.ui.lib.world.EventListener;
import ca.shu.ui.lib.world.IWorldLayer;
import ca.shu.ui.lib.world.IWorldObject;
import ca.shu.ui.lib.world.PaintContext;
import ca.shu.ui.lib.world.piccolo.primitives.PXNode;
import ca.shu.ui.lib.world.piccolo.primitives.PiccoloNodeInWorld;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PInterpolatingActivity;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * World objects are visible UI objects which exist in a World layer (Ground or
 * Sky).
 * 
 * @author Shu Wu
 */
public class WorldObjectImpl implements IWorldObject {
	private static Hashtable<String, EventType> EVENT_CONVERSION_TABLE_1;

	private static Hashtable<EventType, String> EVENT_CONVERSION_TABLE_2;

	private static final long serialVersionUID = 1L;

	public static final Object[][] CONVERSION_MAP = new Object[][] {
			{ EventType.BOUNDS_CHANGED, PNode.PROPERTY_BOUNDS },
			{ EventType.PARENTS_BOUNDS, PXNode.PROPERTY_PARENT_BOUNDS },
			{ EventType.FULL_BOUNDS, PXNode.PROPERTY_FULL_BOUNDS },
			{ EventType.GLOBAL_BOUNDS, PXNode.PROPERTY_GLOBAL_BOUNDS },
			{ EventType.VIEW_TRANSFORM, PCamera.PROPERTY_VIEW_TRANSFORM } };

	public static final long TIME_BETWEEN_POPUPS = 1500;

	protected static EventType piccoloEventToWorldEvent(String propertyName) {
		if (EVENT_CONVERSION_TABLE_1 == null) {
			EVENT_CONVERSION_TABLE_1 = new Hashtable<String, EventType>();
			for (Object[] conversion : CONVERSION_MAP) {
				EVENT_CONVERSION_TABLE_1.put((String) conversion[1],
						(EventType) conversion[0]);

			}

		}

		return EVENT_CONVERSION_TABLE_1.get(propertyName);
	}

	protected static String worldEventToPiccoloEvent(EventType type) {
		if (EVENT_CONVERSION_TABLE_2 == null) {
			EVENT_CONVERSION_TABLE_2 = new Hashtable<EventType, String>();
			for (Object[] conversion : CONVERSION_MAP) {
				EVENT_CONVERSION_TABLE_2.put((EventType) conversion[0],
						(String) conversion[1]);
			}
		}
		return EVENT_CONVERSION_TABLE_2.get(type);
	}

	private Hashtable<EventType, HashSet<EventListener>> eventListenerMap;

	/**
	 * Whether this object has been destroyed
	 */
	private boolean isDestroyed = false;

	/**
	 * Whether this object is selectable by the Selection handler
	 */
	private boolean isSelectable = true;

	/**
	 * The last time a popup was created
	 */
	private long lastPopupTime = 0;

	/**
	 * This object's name
	 */
	private String myName;

	/**
	 * Piccolo counterpart of this object
	 */
	private PNode myPNode;

	/**
	 * Listeners attached ot this object
	 */
	private Hashtable<EventListener, PiccoloChangeListener> piccoloListeners;

	/**
	 * Whether this node is selected by a handler
	 */
	private boolean isSelected = false;

	protected WorldObjectImpl(String name, PiccoloNodeInWorld pNode) {
		super();

		if (pNode == null) {
			pNode = new PXNode();
		}

		if (!(pNode instanceof PNode)) {
			throw new InvalidParameterException();
		}

		myPNode = (PNode) pNode;

		((PiccoloNodeInWorld) myPNode).setWorldObjectParent(this);

		init(name);
	}

	/**
	 * Creates an unnamed WorldObject
	 */
	public WorldObjectImpl() {
		this("", null);
	}

	public WorldObjectImpl(PiccoloNodeInWorld node) {
		this("", node);
	}

	/**
	 * Creates a named WorldObject
	 * 
	 * @param name
	 *            Name of this object
	 */
	public WorldObjectImpl(String name) {
		this(name, null);
	}

	/**
	 * Initializes this instance
	 * 
	 * @param name
	 *            Name of this Object
	 */
	private void init(String name) {
		this.myName = name;

		// this.setFrameVisible(false);
		this.setSelectable(true);

	}

	protected void firePropertyChange(EventType event) {
		if (eventListenerMap != null) {
			HashSet<EventListener> eventListeners = eventListenerMap.get(event);
			if (eventListeners != null) {
				for (EventListener listener : eventListeners) {
					listener.propertyChanged(event);
				}
			}
		}
	}

	/**
	 * Perform any operations before being destroyed
	 */
	protected void prepareForDestroy() {

	}

	public boolean addActivity(PActivity arg0) {
		return myPNode.addActivity(arg0);
	}

	public void addChild(IWorldObject wo) {
		addChild(wo, -1);
	}

	public void addChild(IWorldObject wo, int index) {
		if (wo instanceof WorldObjectImpl) {
			if (index == -1) {
				myPNode.addChild(((WorldObjectImpl) wo).myPNode);
			} else {
				myPNode.addChild(index, ((WorldObjectImpl) wo).myPNode);
			}
		} else {
			throw new InvalidParameterException("Invalid child object");
		}
	}

	public void addInputEventListener(PInputEventListener arg0) {
		myPNode.addInputEventListener(arg0);
	}

	public void addPropertyChangeListener(EventType eventType,
			EventListener worldListener) {

		if (eventListenerMap == null) {
			eventListenerMap = new Hashtable<EventType, HashSet<EventListener>>();
		}

		HashSet<EventListener> eventListeners = eventListenerMap.get(eventType);
		if (eventListeners == null) {
			eventListeners = new HashSet<EventListener>();
			eventListenerMap.put(eventType, eventListeners);
		}

		eventListeners.add(worldListener);

		/*
		 * If there is an associated piccolo event, add the listener to the
		 * piccolo object as well
		 */
		String piccoloPropertyName = worldEventToPiccoloEvent(eventType);
		if (piccoloPropertyName != null) {

			if (piccoloListeners == null) {
				piccoloListeners = new Hashtable<EventListener, PiccoloChangeListener>();

			}
			PiccoloChangeListener piccoloListener = piccoloListeners
					.get(worldListener);

			if (piccoloListener == null) {
				piccoloListener = new PiccoloChangeListener(worldListener);
				piccoloListeners.put(worldListener, piccoloListener);
			}

			getPiccolo().addPropertyChangeListener(
					worldEventToPiccoloEvent(eventType), piccoloListener);

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#animateToPosition(double,
	 *      double, long)
	 */
	public void animateToPosition(double x, double y, long duration) {
		myPNode.animateToPositionScaleRotation(x, y, myPNode.getScale(),
				myPNode.getRotation(), duration);
	}

	public void animateToPositionScaleRotation(double arg0, double arg1,
			double arg2, double arg3, long arg4) {
		myPNode.animateToPositionScaleRotation(arg0, arg1, arg2, arg3, arg4);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#animateToScale(double, long)
	 */
	public void animateToScale(double scale, long duration) {
		myPNode.animateToPositionScaleRotation(myPNode.getOffset().getX(),
				myPNode.getOffset().getY(), scale, myPNode.getRotation(),
				duration);
	}

	public final void destroy() {
		if (!isDestroyed) {
			isDestroyed = true;

			prepareForDestroy();

			/*
			 * Removes this object from the world and finish any tasks Convert
			 * to array to allow for concurrent modification
			 */
			for (IWorldObject wo : getChildren()) {
				wo.destroy();
			}
			if (myPNode instanceof PXNode) {
				((PXNode) myPNode).destroy();
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#doubleClicked()
	 */
	public void doubleClicked() {
	}

	public Collection<IWorldObject> findIntersectingNodes(Rectangle2D fullBounds) {
		ArrayList<PNode> interesectingNodes = new ArrayList<PNode>();
		myPNode.findIntersectingNodes(fullBounds, interesectingNodes);

		Collection<IWorldObject> interesectingObjects = new ArrayList<IWorldObject>(
				interesectingNodes.size());

		for (PNode node : interesectingNodes) {
			if (node instanceof PiccoloNodeInWorld) {
				interesectingObjects.add(((PiccoloNodeInWorld) node)
						.getWorldObjectParent());
			}

		}
		return interesectingObjects;
	}

	public PBounds getBounds() {
		return myPNode.getBounds();
	}

	public Collection<IWorldObject> getChildren() {
		ArrayList<IWorldObject> objects = new ArrayList<IWorldObject>(
				getPiccolo().getChildrenCount());

		Iterator<?> it = getPiccolo().getChildrenIterator();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof PiccoloNodeInWorld) {
				IWorldObject wo = ((PiccoloNodeInWorld) next)
						.getWorldObjectParent();

				if (wo != null) {
					objects.add(wo);
				}
			}
		}
		return objects;
	}

	public PBounds getFullBounds() {
		return myPNode.getFullBounds();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#getHeight()
	 */
	public double getHeight() {
		return myPNode.getHeight();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.NamedObject#getName()
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#getName()
	 */
	public String getName() {
		return myName;
	}

	public Point2D getOffset() {
		return myPNode.getOffset();
	}

	public IWorldObject getParent() {
		PNode parent = getPiccolo().getParent();
		if (parent != null) {
			return ((PiccoloNodeInWorld) parent).getWorldObjectParent();
		} else {
			return null;
		}
	}

	public PNode getPiccolo() {
		return myPNode;
	}

	public double getScale() {
		return myPNode.getScale();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#getTooltip()
	 */
	public IWorldObject getTooltip() {
		return null;
	}

	public float getTransparency() {
		return myPNode.getTransparency();
	}

	public boolean getVisible() {
		return myPNode.getVisible();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#getWidth()
	 */
	public double getWidth() {
		return myPNode.getWidth();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#getWorld()
	 */
	public WorldImpl getWorld() {
		if (getWorldLayer() != null)
			return getWorldLayer().getWorld();
		else
			return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#getWorldLayer()
	 */
	public IWorldLayer getWorldLayer() {
		PNode node = myPNode;

		while (node != null) {
			IWorldObject wo = ((PiccoloNodeInWorld) node)
					.getWorldObjectParent();

			if (wo instanceof IWorldLayer) {
				return (IWorldLayer) wo;
			}

			node = (PXNode) node.getParent();
		}

		return null;

	}

	public double getX() {
		return myPNode.getX();
	}

	public double getY() {
		return myPNode.getY();
	}

	public Dimension2D globalToLocal(Dimension2D globalDimension) {
		return myPNode.globalToLocal(globalDimension);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#globalToLocal(java.awt.geom.Point2D)
	 */
	public Point2D globalToLocal(Point2D arg0) {
		return myPNode.globalToLocal(arg0);
	}

	public Rectangle2D globalToLocal(Rectangle2D globalPoint) {
		return myPNode.globalToLocal(globalPoint);
	}

	public boolean isAncestorOf(IWorldObject wo) {
		return getPiccolo().isAncestorOf(((WorldObjectImpl) wo).getPiccolo());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#isAnimating()
	 */
	public boolean isAnimating() {
		if (myPNode instanceof PiccoloNodeInWorld) {
			return ((PiccoloNodeInWorld) myPNode).isAnimating();
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#isDestroyed()
	 */
	public boolean isDestroyed() {
		return isDestroyed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#isSelectable()
	 */
	public boolean isSelectable() {
		return isSelectable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#isSelected()
	 */
	public boolean isSelected() {
		return isSelected;
	}

	public void layoutChildren() {

	}

	public Point2D localToGlobal(Point2D arg0) {
		return myPNode.localToGlobal(arg0);
	}

	public Rectangle2D localToGlobal(Rectangle2D arg0) {
		return myPNode.localToGlobal(arg0);
	}

	public Point2D localToParent(Point2D localPoint) {
		return myPNode.localToParent(localPoint);
	}

	public Rectangle2D localToParent(Rectangle2D localRectangle) {
		return myPNode.localToParent(localRectangle);
	}

	public void moveToFront() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#objectToGround(java.awt.geom.Point2D)
	 */
	public Point2D objectToGround(Point2D position) {
		IWorldLayer layer = getWorldLayer();

		myPNode.localToGlobal(position);

		if (layer instanceof WorldSkyImpl) {
			layer.getWorld().getSky().localToView(position);
			return position;
		} else if (layer instanceof WorldGroundImpl) {
			return position;
		}
		return null;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#objectToGround(java.awt.geom.Rectangle2D)
	 */
	public Rectangle2D objectToGround(Rectangle2D rectangle) {
		IWorldLayer layer = getWorldLayer();

		myPNode.localToGlobal(rectangle);

		if (layer instanceof WorldSkyImpl) {
			layer.getWorld().getSky().localToView(rectangle);
			return rectangle;
		} else if (layer instanceof WorldGroundImpl) {
			return rectangle;
		}
		return null;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#objectToSky(java.awt.geom.Point2D)
	 */
	public Point2D objectToSky(Point2D position) {
		IWorldLayer layer = getWorldLayer();

		myPNode.localToGlobal(position);

		if (layer instanceof WorldGroundImpl) {
			layer.getWorld().getSky().viewToLocal(position);
			return position;
		} else if (layer instanceof WorldSkyImpl) {
			return position;
		} else {
			throw new InvalidParameterException();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#objectToSky(java.awt.geom.Rectangle2D)
	 */
	public Rectangle2D objectToSky(Rectangle2D rectangle) {
		IWorldLayer layer = getWorldLayer();

		myPNode.localToGlobal(rectangle);

		if (layer instanceof WorldGroundImpl) {
			layer.getWorld().getSky().viewToLocal(rectangle);
			return rectangle;
		} else if (layer instanceof WorldSkyImpl) {
			return rectangle;
		} else {
			throw new InvalidParameterException();
		}

	}

	public void offset(double dx, double dy) {
		Point2D offset = getOffset();
		offset.setLocation(offset.getX() + dx, offset.getY() + dy);
		setOffset(offset);
	}

	public void paint(PaintContext paintContext) {

	}

	public Point2D parentToLocal(Point2D parentPoint) {
		return myPNode.parentToLocal(parentPoint);
	}

	public Rectangle2D parentToLocal(Rectangle2D parentRectangle) {
		return myPNode.parentToLocal(parentRectangle);
	}

	public void removeAllChildren() {
		myPNode.removeAllChildren();
	}

	public void removeChild(IWorldObject wo) {
		if (wo instanceof WorldObjectImpl) {
			myPNode.removeChild(((WorldObjectImpl) wo).getPiccolo());
		} else {
			throw new InvalidParameterException("Invalid child object");
		}
	}

	public void removeFromParent() {
		myPNode.removeFromParent();
	}

	public void removeInputEventListener(PInputEventListener arg0) {
		myPNode.removeInputEventListener(arg0);
	}

	public void removePropertyChangeListener(EventType event,
			EventListener listener) {
		boolean successfull = false;
		if (eventListenerMap != null) {
			HashSet<EventListener> eventListeners = eventListenerMap.get(event);

			if (eventListeners != null) {
				if (eventListeners.contains(listener)) {
					eventListeners.remove(listener);
					successfull = true;
				}

				if (piccoloListeners != null) {
					PiccoloChangeListener piccoloListener = piccoloListeners
							.get(listener);

					if (piccoloListener != null) {
						getPiccolo().removePropertyChangeListener(
								worldEventToPiccoloEvent(event),
								piccoloListener);
					}
				}
			}
		}
		if (!successfull) {
			throw new InvalidParameterException("Listener is not attached");
		}
	}

	public void repaint() {
		myPNode.repaint();
	}

	public boolean setBounds(double arg0, double arg1, double arg2, double arg3) {
		return myPNode.setBounds(arg0, arg1, arg2, arg3);
	}

	public boolean setBounds(Rectangle2D arg0) {
		return myPNode.setBounds(arg0);
	}

	public void setChildrenPickable(boolean areChildrenPickable) {
		myPNode.setChildrenPickable(areChildrenPickable);

	}

	public boolean setHeight(double height) {
		return myPNode.setHeight(height);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.myName = name;
	}

	public void setOffset(double arg0, double arg1) {
		myPNode.setOffset(arg0, arg1);
	}

	public void setOffset(Point2D arg0) {
		setOffset(arg0.getX(), arg0.getY());
	}

	public void setPaint(Paint arg0) {
		myPNode.setPaint(arg0);
	}

	public void setPickable(boolean isPickable) {
		myPNode.setPickable(isPickable);
	}

	public void setScale(double arg0) {
		myPNode.setScale(arg0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#setSelectable(boolean)
	 */
	public void setSelectable(boolean isSelectable) {
		this.isSelectable = isSelectable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#setSelected(boolean)
	 */
	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public void setTransparency(float zeroToOne) {
		myPNode.setTransparency(zeroToOne);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#setVisible(boolean)
	 */
	public void setVisible(boolean isVisible) {
		myPNode.setVisible(isVisible);
	}

	public boolean setWidth(double width) {
		return myPNode.setWidth(width);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.shu.ui.lib.world.impl.IWorldObject#showPopupMessage(java.lang.String)
	 */
	public synchronized void showPopupMessage(String msg) {
		if (getWorld() != null) {

			Util.debugMsg("UI Popup Msg: " + msg);

			TransientMessage msgObject = new TransientMessage(msg);

			double offsetX = -(msgObject.getWidth() - myPNode.getWidth()) / 2d;

			Point2D position = objectToSky(new Point2D.Double(offsetX, -5));

			msgObject.setOffset(position);
			getWorld().getSky().addChild(msgObject);

			long currentTime = System.currentTimeMillis();
			long delay = TIME_BETWEEN_POPUPS - (currentTime - lastPopupTime);

			if (delay < 0) {
				delay = 0;
			}

			msgObject.popup(delay);

			lastPopupTime = currentTime + delay;
		}
	}

	public void translate(double dx, double dy) {
		myPNode.translate(dx, dy);
	}

	public PInterpolatingActivity animateToBounds(double x, double y,
			double width, double height, long duration) {
		return myPNode.animateToBounds(x, y, width, height, duration);
	}

	public double getRotation() {
		return myPNode.getRotation();
	}

}

/**
 * Adapater for WorldObjectChange listener to PropertyChangeListener
 * 
 * @author Shu Wu
 */
class PiccoloChangeListener implements PropertyChangeListener {
	EventListener woChangeListener;

	public PiccoloChangeListener(EventListener worldListener) {
		super();
		this.woChangeListener = worldListener;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		woChangeListener.propertyChanged(WorldObjectImpl
				.piccoloEventToWorldEvent(evt.getPropertyName()));
	}
}