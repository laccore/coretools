package org.andrill.coretools.geology.edit

import org.andrill.coretools.model.Model
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.model.edit.AbstractCommand

import org.andrill.coretools.geology.models.Interval

/**
 * A command for deleting an interval from a container, maintaining contiguity of surrounding intervals
 *
 * @author Brian Grivna
 */
public class DeleteIntervalCommand extends AbstractCommand {
	protected Model model = null;
	protected ModelContainer container = null;
	def above = null, below = null, oldAboveBase = null, oldBelowTop = null

	/**
	 * Create a new DeleteIntervalCommand.
	 *
	 * @param model
	 *            the model.
	 * @param container
	 *            the container.
	 */
	public DeleteIntervalCommand(final Model model, final ModelContainer container) {
		this.model = model;
		this.container = container;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void executeCommand() {
		if (model instanceof Interval) {
			above = container.models.find { it instanceof Interval && it.base == model.top }
			below = container.models.find { it instanceof Interval && it.top == model.base }
			
			if (!above && below) {
				oldBelowTop = below.top
				below.top = model.top
				container.update(below)
			} else if (above && below) {
				// move top down by default
				oldAboveBase = above.base
				above.base = below.top
				container.update(above)
			}
			container.remove(model);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel() {
		return "Delete: " + model.getModelType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void undoCommand() {
		if (!above && below) {
			below.top = oldBelowTop
			container.update(below)
		} else if (above && below) {
			above.base = oldAboveBase
			container.update(above)
		}
		container.add(model);
	}
}