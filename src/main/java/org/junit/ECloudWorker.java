package org.junit;

import org.catid.pipes.PipeListener;
import org.catid.pipes.PipeProtos.TestRequest;
import org.catid.pipes.PipeProtos.TestResult;
import org.catid.pipes.PipeProtos.TestRequest.TestArg;
import org.catid.pipes.PipeReader;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.google.protobuf.InvalidProtocolBufferException;

/*
 * Currently this class only supports working on a single item at a time.
 * It may be slightly more efficient to have two work items queued at a time
 * to avoid delaying the worker.
 */
public class ECloudWorker extends PipeListener {
    ECloudMaster master;
    PipeReader reader;
    String pipeName;
    int requestId;
    TestRequest.Builder requestBuilder = TestRequest.newBuilder();
    TestArg.Builder argBuilder = TestArg.newBuilder();
    ECloudWorkItem workItem;

    @Override
    public void onPipeReady() {
        System.out.println("--- Worker: Ready for connections on " + pipeName);
    }

    @Override
    public void onPipeConnect() {
        System.out.println("--- Worker: Connected on " + pipeName);

        tryDequeueWorkItem();
    }

    @Override
    public void onPipeDisconnect() {
        System.out.println("--- Worker: Disconnected on " + pipeName);

        surrenderWorkItem();
    }

    private void writeRequest(ECloudWorkItem workItem) {
        requestBuilder.setRequestId(++requestId);
        requestBuilder.setClassName(workItem.className);
        requestBuilder.setMethodName(workItem.methodName);

        int index = 0;
        for (String argClassName : workItem.argClassNames) {
            argBuilder.setArgName(argClassName);
            requestBuilder.setArgNames(index++, argBuilder.build());
        }

        TestRequest testRequest = requestBuilder.build();

        reader.writeMessage(testRequest.toByteArray());
    }

    public boolean connected() {
        return reader.connected();
    }
    
    public boolean busy() {
        return workItem != null;
    }

    public boolean tryDequeueWorkItem() {
        ECloudWorkItem newWorkItem = null;

        synchronized (this) {
            if (reader.connected() && workItem == null) {
                newWorkItem = master.pollWork();
                workItem = newWorkItem;
            }
        }

        if (newWorkItem != null) {
            writeRequest(newWorkItem);
            System.out.println("--- Worker: Dequeued work item: " + newWorkItem);
            return true;
        } else {
            return false;
        }
    }

    private void surrenderWorkItem() {
        ECloudWorkItem oldWorkItem = null;

        synchronized (this) {
            if (workItem != null) {
                oldWorkItem = workItem;
                workItem = null;
            }
        }

        if (oldWorkItem != null) {
            System.out.println("--- Worker: Surrendered work item: " + oldWorkItem);

            // Add item back to work queue
            master.addWork(oldWorkItem);
        }
    }

    private void onWorkItemDone(ECloudWorkItem workItem, TestResult result) {
        System.out.println("Work done: " + result);

        switch (result.getResultType()) {
        case 0:
            Description description = (Description)ECloudDaemonRunListener.deserializeObject(result.getResultData());
            workItem.notifier.fireTestRunStarted(description);
            break;
        case 1:
            Result result1 = (Result)ECloudDaemonRunListener.deserializeObject(result.getResultData());
            workItem.notifier.fireTestRunFinished(result1);

            master.onWorkDone(workItem);

            System.out.println("Finished with work item!  Grabbing the next one..");
            this.workItem = null;
            tryDequeueWorkItem();
            break;
        case 2:
            Description description1 = (Description)ECloudDaemonRunListener.deserializeObject(result.getResultData());
            workItem.notifier.fireTestStarted(description1);
            break;
        case 3:
            Description description2 = (Description)ECloudDaemonRunListener.deserializeObject(result.getResultData());
            workItem.notifier.fireTestFinished(description2);

            master.onWorkDone(workItem);

            System.out.println("Finished with work item! (2)  Grabbing the next one..");
            this.workItem = null;
            tryDequeueWorkItem();
            break;
        case 4:
            Failure failure = (Failure)ECloudDaemonRunListener.deserializeObject(result.getResultData());
            workItem.notifier.fireTestFailure(failure);
            break;
        case 5:
            Failure failure1 = (Failure)ECloudDaemonRunListener.deserializeObject(result.getResultData());
            workItem.notifier.fireTestAssumptionFailed(failure1);
            break;
        case 6:
            Description description3 = (Description)ECloudDaemonRunListener.deserializeObject(result.getResultData());
            workItem.notifier.fireTestIgnored(description3);
            break;
        }
    }

    private void onResult(TestResult result) {
        // If request id does not match,
        if (result.getRequestId() != requestId) {
            System.out.println("WARNING: Got back the wrong request id.  Surrendering the work item");

            surrenderWorkItem();
        } else {
            onWorkItemDone(workItem, result);
        }
    }

    @Override
    public void onPipeMessage(byte[] buffer) {
        TestResult result;

        try {
            result = TestResult.parseFrom(buffer);

            onResult(result);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public ECloudWorker(ECloudMaster master, String pipeName) {
        this.master = master;
        this.pipeName = pipeName;

        reader = new PipeReader(pipeName, this, false);
    }

    public void open() {
        reader.open();
    }

    public void join() {
        try {
            reader.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        reader.close();
    }
}
