package org.junit;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.runner.WaitSignal;
import org.junit.runner.notification.RunNotifier;

/*
 * This class coordinates work for several connected daemon instances.
 *
 * It keeps a thread-safe queue of work to perform, and the connection
 * objects will request work when it is available.
 */
public class ECloudMaster extends Thread {
    String pipeBaseName;
    Queue<ECloudWorkItem> workItems = new LinkedList<ECloudWorkItem>();
    ECloudWorker[] workers;
    private static WaitSignal exitSignal = new WaitSignal();
    private static WaitSignal busySignal = new WaitSignal();
    boolean exitFlag;
    int activeRequests;

    private static ECloudMaster instance;

    public static ECloudMaster get() {
        return instance;
    }

    private void signalExit() {
        exitFlag = true;
        exitSignal.doNotify();
    }

    public ECloudMaster(String pipeBaseName) {
        this.pipeBaseName = pipeBaseName;
        instance = this;
    }

    public void addWork(ECloudWorkItem workItem) {
        synchronized (workItems) {
            workItems.add(workItem);
        }

        exitSignal.doNotify();
    }

    public static void waitForWorkAndJoin() {
        ECloudMaster master = instance;
        if (master != null) {
            try {
                System.out.println("----> Waiting for active requests to finish.");
                while (master.activeRequests > 0) {
                    master.busySignal.doWait();
                    System.out.println("----> Woke with " + master.activeRequests + " requests remaining");
                }
                System.out.println("----> Active requests are finished!  Signalling exit...");

                master.close();

                System.out.println("----> Waiting for master to join...");

                master.join();

                System.out.println("----> Full success!  Bye!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void onWorkDone(ECloudWorkItem workItem) {
        --activeRequests;

        System.out.println(">>>> Only " + activeRequests + " requests remaining...");

        if (activeRequests <= 0) {
            busySignal.doNotify();
        }
    }

    public void runByName(String className, String methodName, String []argClassNames, RunNotifier notifier) {
        ECloudWorkItem workItem = new ECloudWorkItem(className, methodName, argClassNames, notifier);

        ++activeRequests;

        System.out.println(">>>> Increased to " + activeRequests + " requests remaining...");

        addWork(workItem);
    }

    public ECloudWorkItem pollWork() {
        ECloudWorkItem workItem = null;

        synchronized (workItems) {
            workItem = workItems.poll();
        }

        return workItem;
    }

    public void close() {
        if (workers != null) {
            signalExit();
        }
    }

    @Override
    public void run() {
        // Start workers here and join them all
        int processors = Runtime.getRuntime().availableProcessors();

        /*
         * TODO: Good idea?  Needs benchmarking -cat
                // Reduce processor count by one if possible
                if (processors > 1) {
                    processors--;
                }
        */

        System.out.println("For processors: " + processors);

        workers = new ECloudWorker[processors];

        // For each processor to launch,
        for (int ii = 0; ii < processors; ++ii) {
            workers[ii] = new ECloudWorker(this, pipeBaseName + ii);
            workers[ii].open();
        }

        // While not exiting,
        while (!exitFlag) {
            // Wait for event
            exitSignal.doWait();

            // If exiting,
            if (exitFlag) {
                break;
            }

            // Start as much new work as possible
            for (int ii = 0; ii < processors; ++ii) {
                ECloudWorker worker = workers[ii];

                worker.tryDequeueWorkItem();
            }
        }

        System.out.println("----> Got exit flag.  Master shutting down workers...");

        for (int ii = 0; ii < processors; ++ii) {
            workers[ii].close();
        }

        System.out.println("----> Waiting for workers to join...");

        // Wait for all processors to join
        for (int ii = 0; ii < processors; ++ii) {
            workers[ii].join();
        }

        workers = null;

        System.out.println("----> All threads joined.  Bye!");
    }
}
