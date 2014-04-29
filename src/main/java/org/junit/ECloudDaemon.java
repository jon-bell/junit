package org.junit;

import java.lang.reflect.Method;
import java.util.List;

import org.catid.pipes.PipeListener;
import org.catid.pipes.PipeProtos.TestRequest;
import org.catid.pipes.PipeProtos.TestRequest.TestArg;
import org.catid.pipes.PipeProtos.TestResult;
import org.catid.pipes.PipeReader;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import com.google.protobuf.InvalidProtocolBufferException;

public class ECloudDaemon extends PipeListener {
    PipeReader reader;
    String pipeName;
    TestResult.Builder resultBuilder = TestResult.newBuilder();
    ECloudDaemonRunListener listener;

    public void onPipeReady() {
        System.out.println("--- Daemon: Ready for connections on " + pipeName);
    }

    public void onPipeConnect() {
        System.out.println("--- Daemon: Connected on " + pipeName);
    }

    public void onPipeDisconnect() {
        System.out.println("--- Daemon: Disconnected on " + pipeName);

        // Do not shut down the pipe on disconnect, just wait for more data
        //reader.close();
    }

    public TestResult.Builder getResultBuilder() {
        return resultBuilder;
    }

    public void writeResult() {
        TestResult testResult = resultBuilder.build();

        reader.writeMessage(testResult.toByteArray());
    }
    
    private Class<?> getPrimitiveType(String name) throws ClassNotFoundException
    {
        if (name.equals("byte"))
            return byte.class;
        if (name.equals("short"))
            return short.class;
        if (name.equals("int"))
            return int.class;
        if (name.equals("long"))
            return long.class;
        if (name.equals("char"))
            return char.class;
        if (name.equals("float"))
            return float.class;
        if (name.equals("double"))
            return double.class;
        if (name.equals("boolean"))
            return boolean.class;
        if (name.equals("void"))
            return void.class;

        return Class.forName(name);
    }

    private void runByName(String className, String methodName, String []argClassNames, RunNotifier notifier) {
        try {
            Class<?> act = Class.forName(className);
            BlockJUnit4ClassRunner tailoredRunner = new BlockJUnit4ClassRunner(act);

            tailoredRunner.runByName(className, methodName, argClassNames, notifier);
        } catch (ClassNotFoundException | SecurityException e) {
            e.printStackTrace();
        } catch (InitializationError e) {
            e.printStackTrace();
        }
    }

    private void serviceRequest(TestRequest request) {
        System.out.println("Got request for " + request);

        resultBuilder.setRequestId(request.getRequestId());

        List<TestArg> argNames = request.getArgNamesList();
        String[] argClassNames = new String[argNames.size()];

        for(int i = 0; i < argNames.size(); i++) {
            argClassNames[i] = argNames.get(i).getArgName();
        }

        String className = request.getClassName();
        String methodName = request.getMethodName();

        RunNotifier notifier = new RunNotifier();
        notifier.addListener(listener);

        runByName(className, methodName, argClassNames, notifier);
    }

    public void onPipeMessage(byte[] buffer) {
        TestRequest request;

        try {
            request = TestRequest.parseFrom(buffer);

            serviceRequest(request);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public ECloudDaemon(String pipeName) {
        this.pipeName = pipeName;

        listener = new ECloudDaemonRunListener(this);
        reader = new PipeReader(pipeName, this, true);
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
