package com.tushar.cmspen2;

import java.io.FileDescriptor;


class Exec
{
    static {
        try {
            System.loadLibrary("EventInjector");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static native FileDescriptor createSubprocess(
            String cmd, String[] args, String[] envVars, int[] processId);


    public static native int waitFor(int processId);


    public static native void close(FileDescriptor fd);


    public static native void hangupProcessGroup(int processId);
}