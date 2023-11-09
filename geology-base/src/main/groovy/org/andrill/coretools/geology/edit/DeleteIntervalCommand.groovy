package org.andrill.coretools.geology.edit

import org.andrill.coretools.model.Model
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.model.edit.AbstractCommand

import org.andrill.coretools.geology.models.Interval

import org.andrill.coretools.geology.models.csdf.*

/**
 * A command for deleting an interval from a container while maintaining contiguity of remaining intervals where possible.
 * Generalized to handle Origin.TOP or Origin.BASE. Vars with the "next" prefix refer to the direction
 * *away* from the origin, "prev" toward it.  That is:
 * - For Origin.TOP, "next" implies models below, "prev" above.
 * - For Origin.BASE, "next" implies models above, "prev" below.
 * 
 * @author Brian Grivna
 */
public class DeleteIntervalCommand extends AbstractCommand {
	protected Model model = null;
	protected ModelContainer container = null;
	protected String nextProp = null, prevProp = null;
	def nextModel = null, prevModel = null, nextDepth = null, prevDepth = null

	/**
	 * Create a new DeleteIntervalCommand.
	 *
	 * @param model
	 *            the model.
	 * @param container
	 *            the container.
	 */
	public DeleteIntervalCommand(final Model model, final ModelContainer container, final boolean originTop) {
		this.model = model;
		this.container = container;
		this.nextProp = originTop ? "top" : "base"
		this.prevProp = originTop ? "base" : "top"
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void executeCommand() {
		nextModel = findNextModel(model)
		if (nextModel) {
			prevModel = findPrevModel(model)
			if (prevModel) { // models are above and below, expand previous model to fill void
				prevDepth = prevModel."$prevProp"
				prevModel."$prevProp" = model."$prevProp"
				container.update(prevModel)
			} else { // this model is nearest the origin, expand next model to fill void
				nextDepth = nextModel."$nextProp"
				nextModel."$nextProp" = model."$nextProp"
				container.update(nextModel)
			}
		} // else this model is farthest from the origin, just delete
		container.remove(model)
	}
	
	private findNextModel(model) {
		def nextModel = container.models.find { it.class == model.class && it."$nextProp" == model."$prevProp" }
		return nextModel
	}
	
	private findPrevModel(model) {
		def prevModel = container.models.find { it.class == model.class && it."$prevProp" == model."$nextProp" }
		return prevModel
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
		if (nextModel) {
			if (prevModel) {
				prevModel."$prevProp" = prevDepth
				container.update(prevModel)
			} else {
				nextModel."$nextProp" = nextDepth
				container.update(nextModel)
			}
		}
		container.add(model)
	}
}