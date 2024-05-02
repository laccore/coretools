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
package org.andrill.coretools.scene;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.Class;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.andrill.coretools.Adaptable;
import org.andrill.coretools.AdapterManager;
import org.andrill.coretools.Platform;
import org.andrill.coretools.graphics.GraphicsContext;
import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.edit.CommandStack;
import org.andrill.coretools.scene.event.DefaultSceneEventHandler;
import org.andrill.coretools.scene.event.SceneEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * The default {@link Scene} implementation.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DefaultScene implements Scene, ModelContainer.Listener, LabelProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScene.class);
	private static final String ORIGIN_PROP = "origin";

	protected ModelContainer models = null;
	protected Map<Track, String> constraints = new HashMap<Track, String>();
	protected Rectangle contents = new Rectangle();
	protected int footerHeight = 36;
	private SceneEventHandler handler;
	protected int headerHeight = 36;
	protected Map<Track, Rectangle> layout = new HashMap<Track, Rectangle>();
	protected List<Track> tracks = new ArrayList<Track>();
	private AtomicBoolean valid = new AtomicBoolean(false);
	protected List<ChangeListener> changeListeners = new CopyOnWriteArrayList<ChangeListener>();
	protected List<SelectionListener> selectionListeners = new CopyOnWriteArrayList<SelectionListener>();
	protected Selection selection = Selection.EMPTY;
	protected CommandStack commandStack = null;
	protected Map<String, String> hints = new HashMap<String, String>();
	protected int preferredWidth = -1;
	protected final AdapterManager manager;
	protected Map<String, String> parameters = new HashMap<String, String>();

	/**
	 * Create a new DefaultScene.
	 */
	public DefaultScene() {
		this(Origin.TOP, Platform.getService(AdapterManager.class));
	}

	/**
	 * Create a new DefaultScene with the specified origin.
	 * 
	 * @param origin
	 *            the origin.
	 */
	public DefaultScene(final Origin origin) {
		this(origin, Platform.getService(AdapterManager.class));
	}

	/**
	 * Creates a new DefaultScene.
	 * 
	 * @param origin
	 *            the origin.
	 * @param manager
	 *            the adapter manager.
	 */
	public DefaultScene(final Origin origin, final AdapterManager manager) {
		this.manager = manager;
		setOrigin(origin);
	}

	/**
	 * Create a new DefaultScene from the specified scene.
	 * 
	 * @param scene
	 *            the scene.
	 */
	public DefaultScene(final Scene scene) {
		this(scene, Platform.getService(AdapterManager.class));
	}

	/**
	 * Create a new DefaultScene from the specified scene.
	 * 
	 * @param scene
	 *            the scene.
	 * @param manager
	 *            the manager.
	 */
	public DefaultScene(final Scene scene, final AdapterManager manager) {
		this.manager = manager;
		setOrigin(scene.getOrigin());
		setModels(scene.getModels());
		setPreferredWidth(scene.getPreferredWidth());
		setScalingFactor(scene.getScalingFactor());
		for (Track t : scene.getTracks()) {
			try {
				addTrack(t.getClass().newInstance(), scene.getTrackConstraints(t));
			} catch (InstantiationException e) {
				LOGGER.error("Unable to clone track {}: {}", t.getClass().getName(), e.getMessage());
			} catch (IllegalAccessException e) {
				LOGGER.error("Unable to clone track {}: {}", t.getClass().getName(), e.getMessage());
			}
		}
		hints.putAll(scene.getRenderHints());
		parameters.putAll(scene.getParameters());
	}

	/**
	 * {@inheritDoc}
	 */
	public void addChangeListener(final ChangeListener l) {
		if (!changeListeners.contains(l)) {
			changeListeners.add(l);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void addSelectionListener(final SelectionListener l) {
		if (!selectionListeners.contains(l)) {
			selectionListeners.add(l);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void addTrack(final Track track, final String constraints) {
		tracks.add(track);
		track.setScene(this);
		track.setModels(models);
		if (constraints != null) {
			this.constraints.put(track, constraints);
		}
		layout.put(track, new Rectangle());
		invalidate();
	}

	protected SceneEventHandler createHandler() {
		return new DefaultSceneEventHandler(this, layout);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object findAt(final Point2D screen, final ScenePart part) {
		Track track = findTrack(screen, part);
		if (track == null) {
			return this;
		} else {
			Object target = track.findAt(screen, part);
			return target == null ? track : target;
		}
	}

	public Track findTrack(final Point2D screen, final ScenePart part) {
		for (Entry<Track, Rectangle> e : layout.entrySet()) {
			Rectangle r = e.getValue();
			if ((screen.getX() >= r.getMinX()) && (screen.getX() <= r.getMaxX())) {
				return e.getKey();
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> E getAdapter(final Class<E> adapter) {
		if (adapter == SceneEventHandler.class) {
			if (handler == null) {
				handler = createHandler();
			}
			return adapter.cast(handler);
		} else if (adapter == LabelProvider.class) {
			return adapter.cast(this);
		} else if (manager != null) {
			return manager.getAdapter(this, adapter);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public CommandStack getCommandStack() {
		return commandStack;
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getContentSize() {
		validate();
		return contents;
	}

	public List<Class> getCreatedClasses() {
		List<Class> classes = new ArrayList<Class>();
		for (Track t : tracks) {
			classes.addAll(t.getCreatedClasses());
		}
		return classes;
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getFooterSize() {
		validate();
		return new Rectangle2D.Double(0, 0, contents.width, footerHeight);
	}

	protected SceneEventHandler getHandler() {
		if (handler == null) {
			handler = new DefaultSceneEventHandler(this, layout);
		}
		return handler;
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getHeaderSize() {
		validate();
		return new Rectangle2D.Double(0, 0, contents.width, headerHeight);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel(final Point2D screen, final ScenePart part) {
		LabelProvider labelProvider = null;
		Track track = findTrack(screen, part);
		if (track == null) {
			return null;
		}

		Object item = track.findAt(screen, part);
		if ((item != null) && (item instanceof Adaptable)) {
			labelProvider = ((Adaptable) item).getAdapter(LabelProvider.class);
		}
		if ((labelProvider == null) && (track != null)) {
			labelProvider = track.getAdapter(LabelProvider.class);
		}

		if (labelProvider == null) {
			return null;
		} else {
			return labelProvider.getLabel(screen, part);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ModelContainer getModels() {
		return models;
	}

	/**
	 * {@inheritDoc}
	 */
	public Origin getOrigin() {
		if (Origin.TOP.name().equalsIgnoreCase(getProperty(ORIGIN_PROP, "top"))) {
			return Origin.TOP;
		} else {
			return Origin.BASE;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getParameter(final String name, final String defaultValue) {
		String value = parameters.get(name);
		if ((value == null) || "".equals(value.trim())) {
			return defaultValue;
		} else {
			return value;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableMap<String, String> getParameters() {
		return ImmutableMap.copyOf(parameters);
	}

	/**
	 * {@inheritDoc}
	 */
	public double getPreferredWidth() {
		return preferredWidth;
	}

	protected String getProperty(final String name, final String defaultValue) {
		if (hints.containsKey(name)) {
			return hints.get(name);
		} else {
			return defaultValue;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getRenderHint(final String name) {
		return hints.get(name);
	}

	/**
	 * Gets the render hints.
	 * 
	 * @return the render hints.
	 */
	public Map<String, String> getRenderHints() {
		return hints;
	}

	/**
	 * {@inheritDoc}
	 */
	public double getScalingFactor() {
		return parse(getProperty("scale", "1"));
	}

	/**
	 * {@inheritDoc}
	 */
	public Selection getSelection() {
		return selection;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTrackConstraints(final Track track) {
		return constraints.get(track);
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableList<Track> getTracks() {
		return ImmutableList.copyOf(tracks);
	}

	/**
	 * {@inheritDoc}
	 */
	public void invalidate() {
		if (valid.getAndSet(false)) {
			for (ChangeListener l : changeListeners) {
				l.sceneChanged();
			}
		}
	}

	protected void layoutTracks() {
		int width = 0;
		int minContent = Integer.MAX_VALUE;
		int maxContent = Integer.MIN_VALUE;
		int expandable = 0;

		// Step 1: calculate width from constraints or default track width
		for (Track t : tracks) {
			Rectangle2D size = t.getContentSize();
			Rectangle lr = layout.get(t);

			// get our constraint
			String constraint = constraints.get(t);
			if ((constraint != null) && (constraint.indexOf('*') > -1)) {
				expandable++;
			}

			// set our coordinates
			lr.x = width;
			lr.y = 0;

			// calculate our width
			int w = parseConstraint(constraint);
			if (w > 0) {
				lr.width = w;
			} else {
				lr.width = (int) Math.ceil(size.getWidth());
			}
			width += lr.width;

			if (size.getHeight() >= 0) {
				minContent = (int) Math.min(minContent, Math.floor(size.getMinY()));
				maxContent = (int) Math.max(maxContent, Math.ceil(size.getMaxY()));
			}
		}

		// adjust the track widths to fit the preferred width
		if ((preferredWidth > 0) && (expandable > 0)) {
			int adjust = preferredWidth - width;
			if (adjust >= 0) {
				adjust = adjust / expandable;
			} else {
				adjust = adjust / tracks.size();
			}
			width = 0;

			// adjust the tracks
			for (Track t : tracks) {
				Rectangle lr = layout.get(t);
				String constraint = constraints.get(t);

				lr.x = width;
				if (adjust < 0) {
					lr.width = lr.width + adjust;
				} else if ((constraint != null) && (constraint.indexOf('*') > -1)) {
					lr.width = lr.width + adjust;
				}
				width += lr.width;
			}
			width = preferredWidth;
		}

		if (minContent == Integer.MAX_VALUE) {
			minContent = 0;
		}
		if (maxContent == Integer.MIN_VALUE) {
			maxContent = 0;
		}

		contents.width = width;
		contents.y = minContent;
		contents.height = maxContent - minContent;
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelAdded(final Model model) {
		invalidate();
		setSelection(new Selection(model));
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelRemoved(final Model model) {
		if (selection.getFirstObject() == model) {
			setSelection(null);
		}
		invalidate();
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelUpdated(final Model model) {
		invalidate();
	}

	protected double parse(final String number) {
		try {
			return Double.parseDouble(number);
		} catch (final NumberFormatException nfe) {
			return -1;
		}
	}

	protected int parseConstraint(final String constraint) {
		if ((constraint == null) || constraint.contains("*")) {
			return -1;
		} else if (constraint.contains("in")) {
			return (int) Math.ceil(parse(constraint.replace("in", "").trim()) * 72);
		} else {
			return (int) Math.ceil(parse(constraint));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeChangeListener(final ChangeListener l) {
		changeListeners.remove(l);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeSelectionListener(final SelectionListener l) {
		selectionListeners.remove(l);
	}

	/**
	 * Remove a track from the scene.
	 * 
	 * @param track
	 *            the track to remove.
	 */
	public void removeTrack(final Track track) {
		if (tracks.remove(track)) {
			track.setScene(null);
			track.setModels(null);
			constraints.remove(track);
			layout.remove(track);
			invalidate();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderContents(final GraphicsContext graphics, final Rectangle2D clip) {
		int x = 0;
		Rectangle2D r = (clip == null) ? contents : clip;
		graphics.pushTransform(AffineTransform.getTranslateInstance(0, -r.getMinY()));
		for (Track t : tracks) {
			int w = layout.get(t).width;
			Rectangle bounds = new Rectangle(x, (int) Math.floor(r.getY()), w, (int) Math.ceil(r.getHeight()));
			graphics.setClip(bounds);
			graphics.pushState();
			t.renderContents(graphics, bounds);
			graphics.popState();
			graphics.setClip(null);
			if (shouldRenderBorders()) {
				graphics.drawRectangle(bounds);
			}
			x += w;
		}
		graphics.popTransform();
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderFooter(final GraphicsContext graphics) {
		int x = 0;
		for (Track t : tracks) {
			int w = layout.get(t).width;
			Rectangle bounds = new Rectangle(x, 1, w, footerHeight - 1);
			graphics.setClip(bounds);
			graphics.pushState();
			t.renderFooter(graphics, bounds);
			graphics.popState();
			graphics.setClip(null);
			if (shouldRenderBorders()) {
				graphics.drawRectangle(bounds);
			}
			x += w;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderHeader(final GraphicsContext graphics) {
		int x = 0;
		for (Track t : tracks) {
			int w = layout.get(t).width;
			Rectangle bounds = new Rectangle(x, 0, w, headerHeight - 1);
			graphics.setClip(bounds);
			graphics.pushState();
			t.renderHeader(graphics, bounds);
			graphics.popState();
			graphics.setClip(null);
			if (shouldRenderBorders()) {
				graphics.drawRectangle(bounds);
			}
			x += w;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCommandStack(final CommandStack commandStack) {
		this.commandStack = commandStack;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setModels(final ModelContainer models) {
		if (this.models != null) {
			this.models.removeListener(this);
		}
		this.models = models;
		for (Track t : tracks) {
			t.setModels(models);
		}
		if (models != null) {
			models.addListener(this);
		}
		invalidate();
	}

	/**
	 * Sets the origin of this scene.
	 * 
	 * @param origin
	 *            the origin.
	 */
	public void setOrigin(final Origin origin) {
		setRenderHint(ORIGIN_PROP, origin.name().toLowerCase());
		invalidate();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setParameter(final String name, final String value) {
		if ((value == null) || "".equals(value.trim())) {
			parameters.remove(name);
		} else {
			parameters.put(name, value);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPreferredWidth(final double width) {
		preferredWidth = (int) Math.ceil(width);
		invalidate();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRenderHint(final String name, final String value) {
		hints.put(name, value);
	}

	/**
	 * Sets the scaling factor.
	 * 
	 * @param scalingFactor
	 *            the scaling factor.
	 */
	public void setScalingFactor(final double scalingFactor) {
		setRenderHint("scale", "" + scalingFactor);
		invalidate();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSelection(final Selection selection) {
		Selection old = this.selection;
		this.selection = (selection == null) ? Selection.EMPTY : selection;
		if (!old.equals(this.selection)) {
			LOGGER.debug("Selection changed {}", this.selection);
			for (SelectionListener l : selectionListeners) {
				LOGGER.debug("Notifying {} of selection change", l);
				l.selectionChanged(this.selection);
			}
		}
	}

	private boolean shouldRenderBorders() {
		return Boolean.parseBoolean(getProperty("borders", "true"));
	}

	/**
	 * {@inheritDoc}
	 */
	public void validate() {
		if (!valid.getAndSet(true)) {
			layoutTracks();
		}
	}
}
