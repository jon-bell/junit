package org.junit;

public class ECloudDaemonMain {
    public static void main(String[] args) {
        System.out.println("ECloud Daemon starting...");

        if (args.length != 1) {
            System.out.println("Input is invalid: This app requires a single argument which will be the name of the named pipe for comms");
            return;
        }

        String pipeBaseName = args[0];

        System.out.println("Using pipe base name: " + pipeBaseName);

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

        ECloudDaemon[] servers = new ECloudDaemon[processors];

        // For each processor to launch,
        for (int ii = 0; ii < processors; ++ii) {
            servers[ii] = new ECloudDaemon(pipeBaseName + ii);
            servers[ii].open();
        }

        // Wait for all processors to halt
        for (int ii = 0; ii < processors; ++ii) {
            servers[ii].join();
        }

        System.out.println("All server threads joined.  Terminating");
    }
}
