package ca.shu.ui.lib.objects;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;

import javax.swing.JPopupMenu;

import ca.neo.ui.style.Style;
import ca.shu.ui.lib.handlers.EventConsumer;
import ca.shu.ui.lib.handlers.Interactable;
import ca.shu.ui.lib.objects.widgets.AffinityHalo;
import ca.shu.ui.lib.objects.widgets.BoundsHandle;
import ca.shu.ui.lib.util.UIEnvironment;
import ca.shu.ui.lib.world.WorldObject;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;

public class Window extends WorldObject {
	public static final WindowState WINDOW_STATE_DEFAULT = WindowState.WINDOW;

	private static final int DEFAULT_HEIGHT = 400;

	private static final int DEFAULT_WIDTH = 600;

	private static final long serialVersionUID = 1L;

	private AffinityHalo affinityHalo = null;

	// private WorldObject attachTo;
	private WeakReference<WorldObject> attachToRef;

	private WorldObject contentNode;

	private PInputEventListener eventConsumer;

	private PPath menubar;

	private MenuBarHandler menuBarHandler;

	private PBounds savedWindowBounds;

	private Point2D savedWindowOffset;;

	private WindowState windowState = WINDOW_STATE_DEFAULT;

	final int MENU_BAR_HEIGHT = 27;

	/**
	 * 
	 * @param attachTo
	 *            parent Node to attach this Window to
	 * @param contentNode
	 *            Node containing the contents of this Window
	 */
	public Window(WorldObject attachTo, WorldObject contentNode) {
		super();
		attachToRef = new WeakReference<WorldObject>(attachTo);

		this.contentNode = contentNode;
		this.setWidth(DEFAULT_WIDTH);
		this.setHeight(DEFAULT_HEIGHT);

		menubar = new MenuBar(this);

		setFrameVisible(true);
		addChild(menubar);
		addChild(contentNode);
		menuBarHandler = new MenuBarHandler(this);
		// menubar.addInputEventListener(menuBarHandler);

		eventConsumer = new EventConsumer();
		// addInputEventListener(eventConsumer);
		windowStateChanged();
	}

	/**
	 * Closes the window
	 */
	public void close() {
		destroy();
	}

	/**
	 * Decreases the size of the window through state transitions
	 */
	public void decreaseWindowSize() {
		switch (windowState) {
		case MAXIMIZED:
			setWindowState(WindowState.WINDOW);
			break;
		case WINDOW:
			setWindowState(WindowState.MINIMIZED);
			break;
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return contentNode.getName();
	}

	/**
	 * 
	 * @return Node representing the contents of the Window
	 */
	public WorldObject getWindowContent() {
		return contentNode;
	}

	public WindowState getWindowState() {
		return windowState;
	}

	/**
	 * Increases the size of the window through state transitions
	 */
	public void increaseWindowSize() {
		switch (windowState) {

		case WINDOW:
			setWindowState(WindowState.MAXIMIZED);
			break;
		case MINIMIZED:
			setWindowState(WindowState.WINDOW);
		}

	}

	public void setWindowState(WindowState state) {
		if (state != windowState) {

			/*
			 * Saves the window state
			 */
			if (windowState == WindowState.WINDOW) {
				savedWindowBounds = getBounds();
				savedWindowOffset = getOffset();
			}

			windowState = state;
			windowStateChanged();
		}
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();

		// Bounds always start at 0,0
		PBounds bounds = this.getBounds();
		this.translate(bounds.getOrigin().getX(), bounds.getOrigin().getY());
		bounds.setOrigin(0, 0);
		this.setBounds(bounds);
		// System.out.println(getBounds().getOrigin());

		// if (isInitialized) {
		menubar.setBounds(1, 1, getWidth() - 2, MENU_BAR_HEIGHT);

		// border.setBounds(-2, -2, getWidth() + 4, getHeight() + 4);
		contentNode.setBounds(0, 0, getWidth() - 4, getHeight() - 4
				- MENU_BAR_HEIGHT);
		contentNode.setOffset(2, 2 + MENU_BAR_HEIGHT);
		// }
	}

	protected void maximizeBounds() {
		setBounds(parentToLocal(getParent().getBounds()));
	}

	@Override
	protected void parentBoundsChanged() {
		// TODO Auto-generated method stub
		super.parentBoundsChanged();

		if (windowState == WindowState.MAXIMIZED) {
			maximizeBounds();
		}

	}

	@Override
	protected void prepareForDestroy() {
		if (affinityHalo != null) {
			affinityHalo.destroy();
			affinityHalo = null;
		}

		menubar.removeInputEventListener(menuBarHandler);
		removeInputEventListener(eventConsumer);

		super.prepareForDestroy();
	}

	protected void windowStateChanged() {
		switch (windowState) {
		case MAXIMIZED:

			if (affinityHalo != null) {
				affinityHalo.destroy();
				affinityHalo = null;
			}

			setDraggable(false);
			UIEnvironment.getInstance().getWorld().addWindow(this);

			maximizeBounds();

			BoundsHandle.removeBoundsHandlesFrom(this);
			break;
		case WINDOW:
			if (savedWindowBounds != null) {
				setBounds(savedWindowBounds);
				setOffset(savedWindowOffset);
			}
			if (attachToRef.get() != null) {
				setDraggable(true);

				attachToRef.get().addChild(this);

				BoundsHandle.addBoundsHandlesTo(this);
				if (affinityHalo == null) {
					affinityHalo = new AffinityHalo(attachToRef.get(), this);
				}
			}

			break;
		case MINIMIZED:

			if (affinityHalo != null) {
				affinityHalo.destroy();
				affinityHalo = null;
			}
			if (attachToRef.get() != null) {
				attachToRef.get().addChild(this);
			}
			break;
		}
		if (windowState == WindowState.MINIMIZED) {
			setVisible(false);
			setChildrenPickable(false);
			setPickable(false);
		} else {
			setVisible(true);
			setChildrenPickable(true);
			setPickable(true);
		}
	}

	public static enum WindowState {
		MAXIMIZED, MINIMIZED, WINDOW
	}

}

class MenuBar extends PPath {

	private static final long serialVersionUID = 1L;
	GButton maximizeButton, minimizeButton, closeButton;
	GText title;

	Window window;

	public MenuBar(Window window) {
		super(new Rectangle2D.Double(0, 0, 1, 1));
		this.window = window;

		init();
	}

	private void init() {
		title = new GText(window.getName());
		title.setFont(Style.FONT_LARGE);
		addChild(title);

		maximizeButton = new GButton("+", new Runnable() {
			public void run() {
				window.increaseWindowSize();
			}
		});
		maximizeButton.setFont(Style.FONT_BIG);

		minimizeButton = new GButton("-", new Runnable() {
			public void run() {
				window.decreaseWindowSize();
			}
		});
		minimizeButton.setFont(Style.FONT_BIG);

		closeButton = new GButton("X", new Runnable() {
			public void run() {
				window.close();
			}
		});
		closeButton.setFont(Style.FONT_BIG);

		addChild(maximizeButton);
		addChild(minimizeButton);
		addChild(closeButton);
		setPaint(Style.COLOR_BACKGROUND2);
	}

	@Override
	protected void layoutChildren() {
		// TODO Auto-generated method stub
		super.layoutChildren();
		title.setBounds(4, 3, getWidth(), getHeight());

		double buttonX = getWidth() - closeButton.getWidth() - 2;
		closeButton.setOffset(buttonX, 2);
		buttonX -= closeButton.getWidth() - 2;
		maximizeButton.setOffset(buttonX, 2);
		buttonX -= minimizeButton.getWidth() - 2;
		minimizeButton.setOffset(buttonX, 2);
	}

}

class MenuBarHandler extends PBasicInputEventHandler {

	private boolean moveToFrontOnPress = true;

	private Window window;

	public MenuBarHandler(Window window) {
		super();
		this.window = window;
	}

	public boolean getMoveToFrontOnPress() {
		return moveToFrontOnPress;
	}

	@Override
	public void mouseClicked(PInputEvent event) {
		super.mouseClicked(event);

		/*
		 * Opens the context menu associated with the Content Node of the Window
		 * Object
		 */
		if (event.getButton() == MouseEvent.BUTTON3) {
			if (window.getWindowContent() instanceof Interactable) {
				Interactable menuNode = ((Interactable) (window
						.getWindowContent()));

				if (menuNode.isContextMenuEnabled()) {
					JPopupMenu menu = menuNode.showContextMenu(event);

					if (menu != null) {
						menu.setVisible(true);
						MouseEvent e = (MouseEvent) event.getSourceSwingEvent();

						menu.show(e.getComponent(), e.getPoint().x, e
								.getPoint().y);
					}
				}
			}
		}
	}

	public void setMoveToFrontOnPress(boolean moveToFrontOnPress) {
		this.moveToFrontOnPress = moveToFrontOnPress;
	}

}