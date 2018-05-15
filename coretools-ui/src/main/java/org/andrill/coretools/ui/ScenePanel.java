/*
 * Copyright (c) Josh Reed, 2009.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andrill.coretools.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.andrill.coretools.graphics.GraphicsContext;
import org.andrill.coretools.graphics.driver.Java2DDriver;
import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.edit.CommandStack;
import org.andrill.coretools.scene.LabelProvider;
import org.andrill.coretools.scene.Scene;
import org.andrill.coretools.scene.Selection;
import org.andrill.coretools.scene.Scene.Origin;
import org.andrill.coretools.scene.Scene.ScenePart;
import org.andrill.coretools.scene.event.Feedback;
import org.andrill.coretools.scene.event.SceneEventHandler;
import org.andrill.coretools.scene.event.SceneKeyEvent;
import org.andrill.coretools.scene.event.SceneMouseEvent;

/**
 * A panel component for rendering a Scene.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ScenePanel extends JPanel implements MouseListener, MouseMotionListener, KeyListener,
        Scene.ChangeListener, Scene.SelectionListener, ModelContainer.Listener, AdjustmentListener, Scrollable {
	/**
	 * Defines the the two possible rendering orientations for scenes.
	 */
	public enum Orientation {
		VERTICAL, HORIZONTAL
	}

	/**
	 * Provides the selection.
	 */
	public interface SelectionProvider {

		/**
		 * Gets the selection.
		 * 
		 * @param scene
		 *            the scene.
		 * @param event
		 *            the mouse event that triggered a potential selection change.
		 * @return the new selection or null if the selection should not change.
		 */
		Selection getSelection(Scene scene, SceneMouseEvent event);
	}

	public interface KeySelectionProvider {
		Selection getSelection(Scene scene, SceneKeyEvent event);
	}

	private static final long serialVersionUID = 1L;
	private static final SelectionProvider DEFAULT_PROVIDER = new SelectionProvider() {
		public Selection getSelection(final Scene scene, final SceneMouseEvent e) {
			Object o = scene.findAt(new Point2D.Double(e.getX(), e.getY()), e.getTarget());
			return (o == null) ? Selection.EMPTY : new Selection(o);
		}
	};

	private static final KeySelectionProvider DEFAULT_KEY_SELECTION_PROVIDER = new KeySelectionProvider() {
		public Selection getSelection(final Scene scene, final SceneKeyEvent e) {
			return scene.getSelection(); // return existing selection by default
		}
	};

	protected Scene scene = null;
	protected ScenePart part = ScenePart.CONTENTS;
	protected SceneEventHandler handler = null;
	protected Feedback feedback = null;
	protected Orientation orientation;
	protected int padding = 0;
	protected int last = 0;
	protected int height = 0;
	protected AtomicBoolean repainting = new AtomicBoolean(false);
	protected SelectionProvider selectionProvider = DEFAULT_PROVIDER;
	protected KeySelectionProvider keySelectionProvider = DEFAULT_KEY_SELECTION_PROVIDER;
	protected int scrollUnits = 20;
	
	/**
	 * Create a new ScenePanel.
	 */
	public ScenePanel() {
		this(null, ScenePart.CONTENTS, Orientation.VERTICAL, 0);
	}

	/**
	 * Create a new ScenePanel.
	 * 
	 * @param scene
	 *            the scene.
	 * @param part
	 *            the part.
	 * @param orientation
	 *            the orientation.
	 */
	public ScenePanel(final Scene scene, final ScenePart part, final Orientation orientation) {
		this(scene, part, orientation, 0);
	}

	/**
	 * Create a new ScenePanel.
	 * 
	 * @param scene
	 *            the scene.
	 * @param part
	 *            the part.
	 * @param orientation
	 *            the orientation.
	 * @param padding
	 *            the amount of padding to display in scene units.
	 */
	public ScenePanel(final Scene scene, final ScenePart part, final Orientation orientation, final int padding) {
		this.part = part;
		this.orientation = orientation;
		this.padding = padding;
		setScene(scene);
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}

	public void adjustmentValueChanged(final AdjustmentEvent e) {
		if (!repainting.getAndSet(true)) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					repaint(getVisibleRect());
					repainting.set(false);
				}
			});
		}
	}

	private Dimension dim(final Rectangle2D dim, final int padding) {
		if (orientation == Orientation.VERTICAL) {
			return new Dimension((int) Math.ceil(dim.getWidth()), (int) Math.ceil(dim.getHeight() + padding));
		} else {
			return new Dimension((int) Math.ceil(dim.getHeight() + padding), (int) Math.ceil(dim.getWidth()));
		}
	}

	protected Cursor getCursor(final int type) {
		if (orientation == Orientation.HORIZONTAL) {
			switch (type) {
				case Cursor.N_RESIZE_CURSOR:
					return new Cursor(Cursor.W_RESIZE_CURSOR);
				case Cursor.NW_RESIZE_CURSOR:
					return new Cursor(Cursor.SW_RESIZE_CURSOR);
				case Cursor.NE_RESIZE_CURSOR:
					return new Cursor(Cursor.NW_RESIZE_CURSOR);
				case Cursor.E_RESIZE_CURSOR:
					return new Cursor(Cursor.N_RESIZE_CURSOR);
				case Cursor.S_RESIZE_CURSOR:
					return new Cursor(Cursor.E_RESIZE_CURSOR);
				case Cursor.SW_RESIZE_CURSOR:
					return new Cursor(Cursor.SE_RESIZE_CURSOR);
				case Cursor.SE_RESIZE_CURSOR:
					return new Cursor(Cursor.NE_RESIZE_CURSOR);
				case Cursor.W_RESIZE_CURSOR:
					return new Cursor(Cursor.S_RESIZE_CURSOR);
				default:
					return new Cursor(type);
			}
		} else {
			return new Cursor(type);
		}
	}

	/**
	 * Gets the orientation.
	 * 
	 * @return the orientation.
	 */
	public Orientation getOrientation() {
		return orientation;
	}

	public int getPadding() {
		return padding;
	}

	/**
	 * Gets the scene part.
	 * 
	 * @return the scene part.
	 */
	public ScenePart getPart() {
		return part;
	}

	/**
	 * {@inheritDoc}
	 */
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getPreferredSize() {
		if (scene == null) {
			return Scene.ZERO;
		}

		scene.validate();
		switch (part) {
			case HEADER:
				return dim(scene.getHeaderSize(), 0);
			case CONTENTS:
				return dim(scene.getContentSize(), padding);
			case FOOTER:
				return dim(scene.getFooterSize(), 0);
			default:
				return Scene.ZERO;
		}
	}

	/**
	 * Gets the Scene.
	 * 
	 * @return the Scene.
	 */
	public Scene getScene() {
		return scene;
	}

	public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
		if (orientation == SwingConstants.HORIZONTAL) {
			return visibleRect.width - scrollUnits;
		} else {
			return visibleRect.height - scrollUnits;
		}
	}

	public boolean getScrollableTracksViewportHeight() {
		return orientation == Orientation.HORIZONTAL;
	}

	public boolean getScrollableTracksViewportWidth() {
		return orientation == Orientation.VERTICAL;
	}

	public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
		// Get the current position.
		int currentPosition = 0;
		if (orientation == SwingConstants.HORIZONTAL) {
			currentPosition = visibleRect.x;
		} else {
			currentPosition = visibleRect.y;
		}

		// Return the number of pixels between currentPosition
		// and the nearest tick mark in the indicated direction.
		if (direction < 0) {
			int newPosition = currentPosition - (currentPosition / scrollUnits) * scrollUnits;
			return (newPosition == 0) ? scrollUnits : newPosition;
		} else {
			return ((currentPosition / scrollUnits) + 1) * scrollUnits - currentPosition;
		}
	}

	protected String getToolTip(final SceneMouseEvent sme) {
		LabelProvider labelProvider = scene.getAdapter(LabelProvider.class);
		String text = (labelProvider == null ? null : labelProvider.getLabel(
		        new Point2D.Double(sme.getX(), sme.getY()), sme.getTarget()));
		if (text == null) {
			return null;
		} else if (text.contains("\n") || text.contains("/>") || text.contains("</")) {
			return "<html>" + text.replace("\n", "<br/>") + "</html>";
		} else {
			return text;
		}
	}

	protected boolean isEditable() {
		if (scene == null) {
			return false;
		} else {
			CommandStack edit = scene.getCommandStack();
			return ((edit != null) && edit.canExecute());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void keyPressed(final KeyEvent e) {
		if (isFocusOwner() && isEditable()) {
			if (handler != null) {
				SceneKeyEvent ske = new SceneKeyEvent(this, part, e);
				Selection sel = keySelectionProvider.getSelection(scene, ske);
				if (sel != null) {
					scene.setSelection(sel);
					updateFeedback(handler.keyPressed(ske));
				}
			} else {
				updateFeedback(null);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void keyReleased(final KeyEvent e) {
		if (isFocusOwner() && isEditable()) {
			if (handler != null) {
				updateFeedback(handler.keyReleased(new SceneKeyEvent(this, part, e)));
			} else {
				updateFeedback(null);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void keyTyped(final KeyEvent e) {
		if (isFocusOwner() && isEditable()) {
			if (handler != null) {
				updateFeedback(handler.keyTyped(new SceneKeyEvent(this, part, e)));
			} else {
				updateFeedback(null);
			}
		}
	}

	public void modelAdded(final Model model) {
		if ((part == ScenePart.CONTENTS) && (scene != null) && (scene.getOrigin() == Origin.BASE)) {
			int diff = (int) (scene.getContentSize().getHeight() - height);
			Rectangle visible = getVisibleRect();
			if (orientation == Orientation.VERTICAL) {
				scrollRectToVisible(new Rectangle(visible.x, last + diff, visible.width, visible.height / 2));
			} else {
				scrollRectToVisible(new Rectangle(last + diff, visible.y, visible.width / 2, visible.height));
			}
		}
	}

	public void modelRemoved(final Model model) {
		// do nothing
	}

	public void modelUpdated(final Model model) {
		// do nothing
	}

	public void mouseClicked(final MouseEvent e) {
		requestFocusInWindow();
		if (isFocusOwner() && isEditable()) {
			SceneMouseEvent sme;
			if ((handler != null) && ((sme = mouseEvent(e)) != null)) {
				updateFeedback(handler.mouseClicked(sme));
			} else {
				updateFeedback(null);
			}
		}
	}

	public void mouseDragged(final MouseEvent e) {
		requestFocusInWindow();
		if (isFocusOwner() && isEditable()) {
			SceneMouseEvent sme;
			if ((handler != null) && ((sme = mouseEvent(e)) != null)) {
				updateFeedback(handler.mouseDragged(sme));
			} else {
				updateFeedback(null);
			}
		}
	}

	public void mouseEntered(final MouseEvent e) {
		updateFeedback(null);
	}

	private SceneMouseEvent mouseEvent(final MouseEvent e) {
		if (scene == null) {
			return null;
		} else {
			int x = (orientation == Orientation.VERTICAL) ? e.getX()
			        : (int) (getPreferredSize().getHeight() - e.getY());
			int y = (orientation == Orientation.VERTICAL) ? e.getY() : e.getX();
			SceneMouseEvent sme = new SceneMouseEvent(this, part, e);
			sme.setX(x);
			sme.setY(y + (int) Math.ceil(scene.getContentSize().getMinY()));
			return sme;
		}
	}

	public void mouseExited(final MouseEvent e) {
		updateFeedback(null);
	}

	public void mouseMoved(final MouseEvent e) {
		if (scene != null) {
			last = (orientation == Orientation.VERTICAL ? e.getY() : e.getX());
			height = (int) scene.getContentSize().getHeight();
		}
		SceneMouseEvent sme = mouseEvent(e);
		setToolTipText((sme == null ? null : getToolTip(sme)));
		if (isEditable()) {
			if ((handler != null) && (sme != null)) {
				updateFeedback(handler.mouseMoved(sme));
			} else {
				updateFeedback(null);
			}
		}
	}

	public void mousePressed(final MouseEvent e) {
		SceneMouseEvent sme = mouseEvent(e);
		if (sme != null) {
			Selection selection = selectionProvider.getSelection(scene, sme);
			if ((selection != null) && (scene != null)) {
				scene.setSelection(selection);
			}
		}
		if (isEditable()) {
			if ((handler != null) && ((sme) != null)) {
				updateFeedback(handler.mousePressed(sme));
			} else {
				updateFeedback(null);
			}
		}
	}

	public void mouseReleased(final MouseEvent e) {
		requestFocusInWindow();
		if (isEditable()) {
			SceneMouseEvent sme;
			if ((handler != null) && ((sme = mouseEvent(e)) != null)) {
				updateFeedback(handler.mouseReleased(sme));
			} else {
				updateFeedback(null);
			}
		}
	}

	@Override
	protected void paintComponent(final Graphics g) {
		if (scene == null) {
			return;
		}

		// validate the scene
		scene.validate();

		// get our graphics
		final Graphics2D g2d = (Graphics2D) g;

		// clear our clip
		g2d.setBackground(Color.white);
		g2d.setPaint(Color.white);
		g2d.fill(getVisibleRect());

		// get a graphics context
		if (orientation == Orientation.HORIZONTAL) {
			g2d.rotate(-Math.PI / 2);
			g2d.translate(-scene.getContentSize().getWidth(), 0);
		}
		GraphicsContext gfx = new GraphicsContext(new Java2DDriver(g2d, false, this));
		switch (part) {
			case HEADER:
				scene.renderHeader(gfx);
				break;
			case CONTENTS:
				Dimension size = getPreferredSize();
				Rectangle2D sc = scene.getContentSize();
				if (orientation == Orientation.VERTICAL) {
					scene.renderContents(gfx, new Rectangle(0, (int) sc.getY(), size.width, size.height));
				} else {
					scene.renderContents(gfx, new Rectangle(0, (int) sc.getY(), size.height, size.width));
				}
				break;
			case FOOTER:
				scene.renderFooter(gfx);
				break;
		}

		// render any feedback
		if (feedback != null) {
			g2d.translate(0, -scene.getContentSize().getY());
			feedback.renderFeedback(gfx);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void sceneChanged() {
		invalidate();
		Component parent = getParent();
		if (parent != null) {
			parent.invalidate();
			parent.validate();
			parent.repaint();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(final Selection selection) {
		sceneChanged();
	}

	public void setScrollUnitIncrement(final int pixels) {
		scrollUnits = pixels;
	}

	public void setOrientation(final Orientation orientation) {
		this.orientation = orientation;
		if (scene != null) {
			scene.setRenderHint("orientation", orientation.toString().toLowerCase());
		}
		sceneChanged();
	}

	public void setPadding(final int padding) {
		this.padding = padding;
		sceneChanged();
	}

	/**
	 * Sets the scene part.
	 * 
	 * @param part
	 *            the part.
	 */
	public void setPart(final ScenePart part) {
		this.part = part;
		sceneChanged();
	}

	/**
	 * Sets the Scene.
	 * 
	 * @param scene
	 *            the scene.
	 */
	public void setScene(final Scene scene) {
		if (this.scene != null) {
			this.scene.removeChangeListener(this);
			this.scene.removeSelectionListener(this);
			if (this.scene.getModels() != null) {
				this.scene.getModels().removeListener(this);
			}
		}
		this.scene = scene;
		if (scene != null) {
			handler = scene.getAdapter(SceneEventHandler.class);
			scene.addChangeListener(this);
			scene.addSelectionListener(this);
			scene.setRenderHint("orientation", orientation.toString().toLowerCase());
			if (scene.getModels() != null) {
				scene.getModels().addListener(this);
			}
		} else {
			handler = null;
		}
		sceneChanged();
	}

	/**
	 * Sets the selection provider.
	 * 
	 * @param selectionProvider
	 *            the selection provider.
	 */
	public void setSelectionProvider(final SelectionProvider selectionProvider) {
		this.selectionProvider = (selectionProvider == null ? DEFAULT_PROVIDER : selectionProvider);
	}

	public void setKeySelectionProvider(final KeySelectionProvider ksp) {
		this.keySelectionProvider = (ksp == null ? DEFAULT_KEY_SELECTION_PROVIDER : ksp);
	}

	protected void updateFeedback(final Feedback feedback) {
		this.feedback = feedback;
		if ((feedback == null) || !isEditable()) {
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		} else {
			setCursor(getCursor(feedback.getCursorType()));
		}
		sceneChanged();
	}
}
