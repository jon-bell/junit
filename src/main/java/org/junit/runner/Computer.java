package org.junit.runner;

import java.lang.reflect.Method;

import org.junit.ECloudMaster;
import org.junit.internal.RealSystem;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.ParentRunner;

/**
 * Represents a strategy for computing runners and suites.
 * WARNING: this class is very likely to undergo serious changes in version 4.8 and
 * beyond.
 *
 * @since 4.6
 */
public class Computer {
    private final boolean fClasses;
    private final boolean fMethods;
    private ECloudMaster server;
    public static final String pipeBaseName = "ECloudPipe";

    private void initMaster() {
        server = new ECloudMaster(pipeBaseName);
        server.start();
    }
    
    public Computer() {
        fClasses = true;
        fMethods = true;
        initMaster();
	}

    public Computer(boolean classes, boolean methods) {
        fClasses = classes;
        fMethods = methods;
        initMaster();
    }

    public static Computer classes() {
        return new Computer(true, false);
    }

    public static Computer methods() {
        return new Computer(false, true);
    }

    /**
     * Returns a new default computer, which runs tests in serial order
     */
    public static Computer serial() {
        return new Computer();
    }

    private static Runner parallelize(final Runner runner) {
        if (runner instanceof ParentRunner) {
            ((ParentRunner<?>) runner).setScheduler(new RunnerScheduler() {
                @Override
                public void schedule(String className, String methodName, String []argClassNames, RunNotifier notifier) {
                    // TODO: call doNotify() when tests return
                    runner.runByName(className, methodName, argClassNames, notifier);
                }

                @Override
                public void finished() {
                    // Need to wait for all notifiers to finish here!
					System.out.print("AFTER ALL\n");
                }
            });
        }
        return runner;
    }

    private Runner getSuiteComputer(final RunnerBuilder builder,
            Class<?>[] classes) throws InitializationError {
        return new Suite(new RunnerBuilder() {
            @Override
            public Runner runnerForClass(Class<?> testClass) throws Throwable {
                return getRunner(builder, testClass);
            }
        }, classes);
    }

    /**
     * Create a suite for {@code classes}, building Runners with {@code builder}.
     * Throws an InitializationError if Runner construction fails
     */
    public Runner getSuite(final RunnerBuilder builder,
            Class<?>[] classes) throws InitializationError {
        Runner suite = getSuiteComputer(builder, classes);
        return fClasses ? parallelize(suite) : suite;
    }

    protected Runner getRunnerComputer(RunnerBuilder builder, Class<?> testClass) throws Throwable {
        return builder.runnerForClass(testClass);
    }

    /**
     * Create a single-class runner for {@code testClass}, using {@code builder}
     */
    protected Runner getRunner(RunnerBuilder builder, Class<?> testClass) throws Throwable {
        Runner runner = getRunnerComputer(builder, testClass);
        return fMethods ? parallelize(runner) : runner;
    }
}
