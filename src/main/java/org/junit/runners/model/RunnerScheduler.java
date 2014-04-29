package org.junit.runners.model;

import org.junit.runner.notification.RunNotifier;

/**
 * Represents a strategy for scheduling when individual test methods
 * should be run (in serial or parallel)
 *
 * WARNING: still experimental, may go away.
 *
 * @since 4.7
 */
public interface RunnerScheduler {
    /**
     * Schedule a child statement to run
     */
    //void schedule(Runnable childStatement);
    void schedule(String className, String methodName, String[] argClassNames, RunNotifier notifier);

    /**
     * Override to implement any behavior that must occur
     * after all children have been scheduled (for example,
     * waiting for them all to finish)
     */
    void finished();
}
