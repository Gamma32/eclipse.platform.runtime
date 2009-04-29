/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.TestBarrier;

/**
 * A test job that begins and ends a scheduling rule, waiting on signals
 * at each step to allow synchronization with the thread that is running the test.
 */
class JobRuleRunner extends Job {
	private ISchedulingRule rule;
	private TestBarrier barrier;
	private int numRepeats;
	private boolean reportBlocking;

	/**
	 * This job will start applying the given rule in the manager
	 */
	public JobRuleRunner(String name, ISchedulingRule rule, int[] status, int index, int numRepeats, boolean reportBlocking) {
		this(name, rule, new TestBarrier(status, index), numRepeats, reportBlocking);
	}
	/**
	 * This job will start applying the given rule in the manager
	 */
	public JobRuleRunner(String name, ISchedulingRule rule, TestBarrier barrier, int numRepeats, boolean reportBlocking) {
		super(name);
		this.rule = rule;
		this.barrier = barrier;
		this.numRepeats = numRepeats;
		this.reportBlocking = reportBlocking;
	}
	/**
	 * This job will start applying the given rule in the manager
	 */
	public JobRuleRunner(String name, ISchedulingRule rule, TestBarrier barrier) {
		this(name, rule, barrier, 1, false);
	}

	protected IStatus run(IProgressMonitor monitor) {
		//begin executing the job
		monitor.beginTask(getName(), numRepeats);
		try {
			//set the status flag to START
			barrier.setStatus(TestBarrier.STATUS_START);
			for (int i = 0; i < numRepeats; i++) {
				monitor.worked(1);
				//wait until the tester allows this job to run again
				barrier.waitForStatusNoFail(TestBarrier.STATUS_WAIT_FOR_RUN);
				//create a hook that would notify this thread when this job was blocked on a rule (if needed)
				TestBlockingMonitor bMonitor = null;
				if (reportBlocking)
					bMonitor = new TestBlockingMonitor(barrier);

				//start the given rule in the manager
				manager.beginRule(rule, bMonitor);
				//set status to RUNNING
				barrier.setStatus(TestBarrier.STATUS_RUNNING);

				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;

				//wait until tester allows this job to finish
				barrier.waitForStatusNoFail(TestBarrier.STATUS_WAIT_FOR_DONE);
				//end the given rule
				manager.endRule(rule);
				//set status to DONE
				barrier.setStatus(TestBarrier.STATUS_DONE);

				Thread.yield();
			}

		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

}