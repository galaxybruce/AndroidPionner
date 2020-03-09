/**
 * bruce.zhang
 */

package com.galaxybruce.pioneer.utils.runtime;

import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.InputStream;

public class ExecuteResult {
    static final Logger logger = Logger.getLogger(ExecuteResult.class);

    public static int IO_CODE = -1;
    public static int TIMEOUT_CODE = -2;
    public static int EXECUTE_CODE = -3;
    public static int INTERRUPTED_CODE = -4;


    public Process process;
    public int exitCode;
    public String executeOut;
    public String executeErr;

    private InputStream pIn = null;
    private InputStream pErr = null;
    private StreamGobbler outputGobbler = null;
    private StreamGobbler errorGobbler = null;

    public ExecuteResult(Process process) {
        this.process = process;

        if(process != null) {
            pIn = process.getInputStream();
            outputGobbler = new StreamGobbler(pIn, "OUTPUT");
            outputGobbler.start();

            pErr = process.getErrorStream();
            errorGobbler = new StreamGobbler(pErr, "ERROR");
            errorGobbler.start();
        }
    }

    public ExecuteResult(int exitCode, String executeOut, String executeErr) {
        this.exitCode = exitCode;
        this.executeOut = executeOut;
        this.executeErr = executeErr;
    }

    @Override
    public String toString() {
        return "ExecuteResult{" +
                "exitCode=" + exitCode +
                ", executeOut='" + executeOut + '\'' +
                ", executeErr='" + executeErr + '\'' +
                '}';
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;

        // 超时了，说明任务还没结束，不要杀掉
        boolean processExit = process == null || exitCode != ExecuteResult.TIMEOUT_CODE;
        if(processExit) {
            executeOut = outputGobbler.getContent();
            executeErr = errorGobbler.getContent();

            destroyProcess();
        } else {
            executeOut = outputGobbler.getContent(true);
            executeErr = errorGobbler.getContent(true);
        }
    }

    private static void closeQuietly(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
    }

    public void destroyProcess() {
        if (pIn != null) {
            closeQuietly(pIn);
            if (outputGobbler != null && !outputGobbler.isInterrupted()) {
                outputGobbler.interrupt();
            }
        }
        if (pErr != null) {
            closeQuietly(pErr);
            if (errorGobbler != null && !errorGobbler.isInterrupted()) {
                errorGobbler.interrupt();
            }
        }

        if(process != null) {
            process.destroy();
        }
    }
}