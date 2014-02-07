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
package org.andrill.coretools;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * A default implementation of the JobService interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@Singleton
public class DefaultJobService implements JobService {
	private static class PriorityThreadFactory implements ThreadFactory {
		private Priority priority;

		public PriorityThreadFactory(final Priority priority) {
			this.priority = priority;
		}

		public Thread newThread(final Runnable r) {
			Thread thread = new Thread(r);
			switch (priority) {
				case HIGH:
					thread.setPriority(Thread.MAX_PRIORITY);
					break;
				case MEDIUM:
					thread.setPriority(Thread.NORM_PRIORITY);
					break;
				case LOW:
					thread.setPriority(Thread.MIN_PRIORITY);
					break;
				default:
					thread.setPriority(Thread.NORM_PRIORITY);
			}
			return thread;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJobService.class);

	protected ExecutorService high;
	protected ExecutorService medium;
	protected ExecutorService low;

	/**
	 * Create a new DefaultJobService.
	 */
	public DefaultJobService() {
		int size = Runtime.getRuntime().availableProcessors() + 1;
		high = Executors.newFixedThreadPool(size, new PriorityThreadFactory(Priority.HIGH));
		medium = Executors.newFixedThreadPool(size, new PriorityThreadFactory(Priority.MEDIUM));
		low = Executors.newFixedThreadPool(size, new PriorityThreadFactory(Priority.LOW));
		LOGGER.debug("initialized");
	}

	/**
	 * Shuts down the job service.
	 */
	public void shutdown() {
		low.shutdownNow();
		medium.shutdownNow();
		high.shutdownNow();
		LOGGER.debug("shutdown");
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> Future<E> submit(final Callable<E> job, final Priority priority) {
		switch (priority) {
			case HIGH:
				return high.submit(job);
			case MEDIUM:
				return medium.submit(job);
			case LOW:
				return low.submit(job);
			default:
				return low.submit(job);
		}
	}
}
