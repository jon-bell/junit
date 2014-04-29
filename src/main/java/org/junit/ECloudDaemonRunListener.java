package org.junit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.catid.pipes.PipeProtos.TestResult;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.WaitSignal;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

public class ECloudDaemonRunListener extends RunListener {
    ECloudDaemon daemon;

    public ECloudDaemonRunListener(ECloudDaemon daemon) {
        this.daemon = daemon;
    }

    public static String serializeObject(Serializable o) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream( baos );
            oos.writeObject( o );
            oos.close();
            return baos.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object deserializeObject(String serializedObject) {
        byte b[] = serializedObject.getBytes(); 
        ByteArrayInputStream bi = new ByteArrayInputStream(b);
        ObjectInputStream si;
        try {
            si = new ObjectInputStream(bi);
            return si.readObject();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Called before any tests have been run. This may be called on an
     * arbitrary thread.
     *
     * @param description describes the tests to be run
     */
    @Override
    public void testRunStarted(Description description) throws Exception {
        System.out.println("ECloudRunListener: testRunStarted");

        TestResult.Builder resultBuilder = daemon.getResultBuilder();
        resultBuilder.setResultType(0);
        resultBuilder.setResultData(serializeObject(description));
        daemon.writeResult();
    }

    /**
     * Called when all tests have finished. This may be called on an
     * arbitrary thread.
     *
     * @param result the summary of the test run, including all the tests that failed
     */
    @Override
    public void testRunFinished(Result result) throws Exception {
        System.out.println("ECloudRunListener: testRunFinished");

        TestResult.Builder resultBuilder = daemon.getResultBuilder();
        resultBuilder.setResultType(1);
        resultBuilder.setResultData(serializeObject(result));
        daemon.writeResult();
    }

    /**
     * Called when an atomic test is about to be started.
     *
     * @param description the description of the test that is about to be run
     * (generally a class and method name)
     */
    @Override
    public void testStarted(Description description) throws Exception {
        System.out.println("ECloudRunListener: testStarted");

        TestResult.Builder resultBuilder = daemon.getResultBuilder();
        resultBuilder.setResultType(2);
        resultBuilder.setResultData(serializeObject(description));
        daemon.writeResult();
    }

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     *
     * @param description the description of the test that just ran
     */
    @Override
    public void testFinished(Description description) throws Exception {
        System.out.println("ECloudRunListener: testFinished");

        // TODO: Do not need to re-serialize the description over and over
        TestResult.Builder resultBuilder = daemon.getResultBuilder();
        resultBuilder.setResultType(3);
        resultBuilder.setResultData(serializeObject(description));
        daemon.writeResult();
    }

    /**
     * Called when an atomic test fails, or when a listener throws an exception.
     *
     * <p>In the case of a failure of an atomic test, this method will be called
     * with the same {@code Description} passed to
     * {@link #testStarted(Description)}, from the same thread that called
     * {@link #testStarted(Description)}.
     *
     * <p>In the case of a listener throwing an exception, this will be called with
     * a {@code Description} of {@link Description#TEST_MECHANISM}, and may be called
     * on an arbitrary thread.
     *
     * @param failure describes the test that failed and the exception that was thrown
     */
    @Override
    public void testFailure(Failure failure) throws Exception {
        System.out.println("ECloudRunListener: testFailure");

        TestResult.Builder resultBuilder = daemon.getResultBuilder();
        resultBuilder.setResultType(4);
        resultBuilder.setResultData(serializeObject(failure));
        daemon.writeResult();
    }

    /**
     * Called when an atomic test flags that it assumes a condition that is
     * false
     *
     * @param failure describes the test that failed and the
     * {@link org.junit.AssumptionViolatedException} that was thrown
     */
    @Override
    public void testAssumptionFailure(Failure failure) {
        System.out.println("ECloudRunListener: testAssumptionFailure");

        TestResult.Builder resultBuilder = daemon.getResultBuilder();
        resultBuilder.setResultType(5);
        resultBuilder.setResultData(serializeObject(failure));
        daemon.writeResult();
    }

    /**
     * Called when a test will not be run, generally because a test method is annotated
     * with {@link org.junit.Ignore}.
     *
     * @param description describes the test that will not be run
     */
    @Override
    public void testIgnored(Description description) throws Exception {
        System.out.println("ECloudRunListener: testIgnored");

        TestResult.Builder resultBuilder = daemon.getResultBuilder();
        resultBuilder.setResultType(6);
        resultBuilder.setResultData(serializeObject(description));
        daemon.writeResult();
    }
}
