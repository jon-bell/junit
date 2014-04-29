package org.junit.runner;


public class WaitSignal {
    boolean wasSignalled;

    public void reset() {
        wasSignalled = false;
    }
    
    public void doWait() {
        synchronized (this) {
            while (!wasSignalled) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // Ignore spurious wake-ups
                }
            }

            wasSignalled = false;
        }
    }

    public void doNotify() {
        synchronized (this) {
            wasSignalled = true;

            this.notify();
        }
    }
}
