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

import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import org.andrill.coretools.graphics.GraphicsContext;
import org.andrill.coretools.graphics.util.Paper;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.edit.CommandStack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Wraps an existing scene and provides paged rendering.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class PageableScene implements Scene {
	protected final Scene scene;
	protected final Paper paper;
	protected double start = 0;
	protected double perPage = 1;
	protected final boolean renderHeader;
	protected final boolean renderFooter;
	protected final int sectionNameHeight = 20;

	/**
	 * Create a new PageableScene.
	 * 
	 * @param scene
	 *            the scene.
	 * @param paper
	 *            the paper.
	 * @param perPage
	 *            the number of scene units per page.
	 */
	public PageableScene(final Scene scene, final Paper paper, final double perPage) {
		this(scene, paper, scene.getContentSize().getY() / scene.getScalingFactor(), perPage, true, true);
	}

	/**
	 * Create a new PageableScene.
	 * 
	 * @param scene
	 *            the scene.
	 * @param paper
	 *            the paper.
	 * @param start
	 *            the starting value.
	 * @param perPage
	 *            the number of scene units per page.
	 */
	public PageableScene(final Scene scene, final Paper paper, final double start, final double perPage) {
		this(scene, paper, start, perPage, true, true);
	}

	/**
	 * Create a new PageableScene.
	 * 
	 * @param scene
	 *            the scene.
	 * @param paper
	 *            the paper.
	 * @param start
	 *            the starting value.
	 * @param perPage
	 *            the number of scene units per page.
	 * @param renderHeader
	 *            the render header flag.
	 * @param renderFooter
	 *            the render footer flag.
	 */
	public PageableScene(final Scene scene, final Paper paper, final double start, final double perPage,
	        final boolean renderHeader, final boolean renderFooter) {
		this.scene = scene;
		this.paper = paper;
		this.start = start;
		this.renderHeader = renderHeader;
		this.renderFooter = renderFooter;
		setPreferredWidth(paper.getPrintableWidth());
		setPerPage(perPage);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addChangeListener(final ChangeListener l) {
		scene.addChangeListener(l);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addSelectionListener(final SelectionListener l) {
		scene.addSelectionListener(l);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addTrack(final Track track, final String constraints) {
		scene.addTrack(track, constraints);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTrackConstraints(final Track track, final String constraints) {
		scene.setTrackConstraints(track, constraints);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object findAt(final Point2D screen, final ScenePart part) {
		return scene.findAt(screen, part);
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> E getAdapter(final Class<E> adapter) {
		return scene.getAdapter(adapter);
	}

	/**
	 * {@inheritDoc}
	 */
	public CommandStack getCommandStack() {
		return scene.getCommandStack();
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getContentSize() {
		return scene.getContentSize();
	}
	
	public Rectangle2D getContentSize(final int page) {
		Rectangle2D content = scene.getContentSize();
		final double y = (start * scene.getScalingFactor()) + (page - 1) * perPage * scene.getScalingFactor();
		final double height = perPage * scene.getScalingFactor();
		return new Rectangle2D.Double(content.getX(), y, content.getWidth(), height);
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getFooterSize() {
		return scene.getFooterSize();
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getHeaderSize() {
		return scene.getHeaderSize();
	}

	/**
	 * {@inheritDoc}
	 */
	public ModelContainer getModels() {
		return scene.getModels();
	}

	/**
	 * {@inheritDoc}
	 */
	public Origin getOrigin() {
		return scene.getOrigin();
	}

	/**
	 * Gets the number of pages with the specified start Y value in scene coordinates.
	 * 
	 * @return the number of pages.
	 */
	public int getPageCount() {
		Rectangle2D content = scene.getContentSize();
		double height = content.getMaxY() / scene.getScalingFactor() - start;
		return (int) Math.ceil(height / perPage);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getParameter(final String name, final String defaultValue) {
		return scene.getParameter(name, defaultValue);
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableMap<String, String> getParameters() {
		return scene.getParameters();
	}

	/**
	 * Gets the number of scene units visible per page.
	 * 
	 * @return the number of scene units visible per page.
	 */
	public double getPerPage() {
		return perPage;
	}

	/**
	 * {@inheritDoc}
	 */
	public double getPreferredWidth() {
		return scene.getPreferredWidth();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getRenderHint(final String name) {
		return scene.getRenderHint(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, String> getRenderHints() {
		return scene.getRenderHints();
	}

	/**
	 * {@inheritDoc}
	 */
	public double getScalingFactor() {
		return scene.getScalingFactor();
	}

	/**
	 * Get the wrapped scene.
	 * 
	 * @return the scene.
	 */
	public Scene getScene() {
		return scene;
	}

	/**
	 * {@inheritDoc}
	 */
	public Selection getSelection() {
		return scene.getSelection();
	}

	/**
	 * Gets the starting position to begin rendering pages at.
	 * 
	 * @return the starting position in scene units.
	 */
	public double getStart() {
		return start;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTrackConstraints(final Track track) {
		return scene.getTrackConstraints(track);
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableList<Track> getTracks() {
		return scene.getTracks();
	}

	/**
	 * {@inheritDoc}
	 */
	public void invalidate() {
		scene.invalidate();
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeChangeListener(final ChangeListener l) {
		scene.removeChangeListener(l);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeSelectionListener(final SelectionListener l) {
		scene.removeSelectionListener(l);
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderContents(final GraphicsContext graphics, final Rectangle2D clip) {
		scene.renderContents(graphics, clip);
	}

	/**
	 * Render a page of the scene, with the specified page number and starting Y value.
	 * 
	 * @param page
	 *            the page.
	 * @param pageStart
	 *            the starting Y value.
	 * @param graphics
	 *            the graphics.
	 */
	public void renderContents(final int page, final GraphicsContext graphics) {
		scene.setRenderHint("page", "" + page);
		renderContents(graphics, getContentSize(page));
		scene.setRenderHint("page", null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderFooter(final GraphicsContext graphics) {
		scene.renderFooter(graphics);
	}

	/**
	 * Render the footer for the specified page.
	 * 
	 * @param page
	 *            the page.
	 * @param graphics
	 *            the graphics.
	 */
	public void renderFooter(final int page, final GraphicsContext graphics) {
		scene.setRenderHint("page", "" + page);
		renderFooter(graphics);
		scene.setRenderHint("page", null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderHeader(final GraphicsContext graphics) {
		scene.renderHeader(graphics);
	}

	/**
	 * Render the header for the specified page.
	 * 
	 * @param page
	 *            the page.
	 * @param graphics
	 *            the graphics.
	 */
	public void renderHeader(final int page, final GraphicsContext graphics) {
		scene.setRenderHint("page", "" + page);
		renderHeader(graphics);
		scene.setRenderHint("page", null);
	}
	
	public void renderSectionName(final String sectionName, final int page, final GraphicsContext graphics) {
		scene.setRenderHint("page", "" + page);
		graphics.drawString(0, 0, new Font("SanSerif", Font.PLAIN, 12), sectionName);
		scene.setRenderHint("page", null);
	}
	
	public int getSectionNameHeight() { return sectionNameHeight; }

	/**
	 * {@inheritDoc}
	 */
	public void setCommandStack(final CommandStack commandStack) {
		scene.setCommandStack(commandStack);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setModels(final ModelContainer models) {
		scene.setModels(models);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setParameter(final String name, final String value) {
		scene.setParameter(name, value);
	}

	/**
	 * Sets the number of scene units visible per page.
	 * 
	 * @param perPage
	 *            the number of scene units visible per page.
	 */
	public void setPerPage(final double perPage) {
		this.perPage = perPage;
		double content = paper.getPrintableHeight() - getSectionNameHeight();
		if (renderHeader) {
			content -= scene.getHeaderSize().getHeight();
		}
		if (renderFooter) {
			content -= scene.getFooterSize().getHeight();
		}
		scene.setScalingFactor(content / perPage);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPreferredWidth(final double width) {
		scene.setPreferredWidth(width);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRenderHint(final String name, final String value) {
		scene.setRenderHint(name, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setScalingFactor(final double scale) {
		scene.setScalingFactor(scale);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSelection(final Selection selection) {
		scene.setSelection(selection);
	}

	/**
	 * Sets the starting position in scene units.
	 * 
	 * @param start
	 *            the starting position.
	 */
	public void setStart(final double start) {
		this.start = start;
	}

	/**
	 * {@inheritDoc}
	 */
	public void validate() {
		scene.validate();
	}
}
