/**
 * bruce.zhang
 *
 * java Runtime.exec()执行shell/cmd命令：常见的几种陷阱与一种完善实现:
 * https://www.jianshu.com/p/af4b3264bc5d
 */

package com.galaxybruce.pioneer.utils.runtime;

import java.io.IOException;
import java.util.concurrent.*;

public class LocalCommandExecutorImpl implements LocalCommandExecutor {

    static ExecutorService pool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 3L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());

    public static ExecuteResult executeCommand(boolean pcShell, String command, long timeout) {
        int exitCode = 0;
        Process process = null;

        Future<Integer> executeFuture = null;
        ExecuteResult executeResult = null;
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                command = "cmd.exe /c  " + command;
                process = Runtime.getRuntime().exec(command);
            } else {
                if(pcShell) {
                    String[] cmds = { "/bin/sh", "-c", command};
                    process = Runtime.getRuntime().exec(cmds, null, null);
                } else {
                    process = Runtime.getRuntime().exec(command);
                }
            }
            final Process p = process;

            executeResult = new ExecuteResult(p);
            // close process's output stream.
            p.getOutputStream().close();


            // create a Callable for the command's Process which can be called by an Executor
            Callable<Integer> call = new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    p.waitFor();
                    return p.exitValue();
                }
            };

            // submit the command's call and get the result from a
            executeFuture = pool.submit(call);
            exitCode = executeFuture.get(timeout, TimeUnit.MILLISECONDS);
            if(exitCode != 0 || executeResult.executeErr != null && !"".equals(executeResult.executeErr)) {
                String errorMessage = "The command [" + command + "] execute failed: " + executeResult.toString();
                System.out.println(errorMessage);
            }
            return executeResult;

        } catch (IOException ex) {
            exitCode = ExecuteResult.IO_CODE;
            String errorMessage = "The command [" + command + "] execute failed.";
            System.out.println(errorMessage);
            ex.printStackTrace();
            if(executeResult == null) {
                executeResult = new ExecuteResult(exitCode, "", errorMessage);
            }
            return executeResult;
        } catch (TimeoutException ex) {
            exitCode = ExecuteResult.TIMEOUT_CODE;
            String errorMessage = "The command [" + command + "] timed out.";
            System.out.println(errorMessage);
            ex.printStackTrace();
            return executeResult;
        } catch (ExecutionException ex) {
            exitCode = ExecuteResult.EXECUTE_CODE;
            String errorMessage = "The command [" + command + "] did not complete due to an execution error.";
            System.out.println(errorMessage);
            ex.printStackTrace();
            return executeResult;
        } catch (InterruptedException ex) {
            exitCode = ExecuteResult.INTERRUPTED_CODE;
            String errorMessage = "The command [" + command + "] did not complete due to an interrupted error.";
            System.out.println(errorMessage);
            ex.printStackTrace();
            return executeResult;
        } finally {
            if (executeFuture != null) {
                try {
                    executeFuture.cancel(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if(executeResult != null) {
                executeResult.setExitCode(exitCode);
            }
        }
    }


}
