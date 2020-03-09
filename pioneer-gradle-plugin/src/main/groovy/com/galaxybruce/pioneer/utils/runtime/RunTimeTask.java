/**
 * bruce.zhang
 */

package com.galaxybruce.pioneer.utils.runtime;


public class RunTimeTask {

    public static ExecuteResult executeCommand(String command) {
        return LocalCommandExecutorImpl.executeCommand(false, command, 5000L);
    }

    public static ExecuteResult executeCommand(String command, long timeout) {
        return LocalCommandExecutorImpl.executeCommand(false, command, timeout);
    }

    public static ExecuteResult executeCommandWithPcShell(String command) {
        return LocalCommandExecutorImpl.executeCommand(true, command, 5000L);
    }

    public static ExecuteResult executeCommandWithPcShell(String command, long timeout) {
        return LocalCommandExecutorImpl.executeCommand(true, command, timeout);
    }
}
