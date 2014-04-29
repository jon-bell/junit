package org.junit;

import org.junit.runner.notification.RunNotifier;

/*
 * This represents one unit of work.
 */
public class ECloudWorkItem {
    String className;
    String methodName;
    String []argClassNames;
    RunNotifier notifier;

    public ECloudWorkItem(String className, String methodName, String []argClassNames, RunNotifier notifier) {
        this.className = className;
        this.methodName = methodName;
        this.argClassNames = argClassNames;
        this.notifier = notifier;
    }
}
