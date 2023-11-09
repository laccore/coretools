package org.andrill.coretools.geology.edit

import org.andrill.coretools.model.Model
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.model.edit.AbstractCommand

import org.andrill.coretools.geology.models.Interval

/**
 * A command for splitting an interval in two
 *
 * @author Brian Grivna
 */
public class SplitIntervalCommand extends AbstractCommand {
	protected Model model = null;
	protected ModelContainer container = null;
	def newInterval = null

	/**
	 * Create a new SplitIntervalCommand.
	 *
	 * @param model
	 *            the model.
	 * @param container
	 *            the container.
	 */
	public SplitIntervalCommand(final Model model, final ModelContainer container) {
		this.model = model;
		this.container = container;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void executeCommand() {
		final splitDepth = model.top + (model.base - model.top).divide(2.0)
		newInterval = (model.class).newInstance()
		newInterval.top = model.top
		newInterval.base = splitDepth

		model.modelData.keySet().each { prop ->
			if (!(prop in ['top', 'base']) && newInterval.properties.containsKey(prop)) {
				newInterval."$prop" = model."$prop"
			}
		}

		model.top = splitDepth

		container.add(newInterval)
		container.update(model)
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel() {
		return "Split: " + model.getModelType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void undoCommand() {
		model.top = newInterval.top
		container.remove(newInterval)
		container.update(model)
	}
}