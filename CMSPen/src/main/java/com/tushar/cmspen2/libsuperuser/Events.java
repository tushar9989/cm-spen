package com.tushar.cmspen2.libsuperuser;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.util.Log;


public class Events
{
	
	private final static String					LT = "CMSPen";
	
	public class InputDevice {
		
		private int m_nId;
		private String m_szPath, m_szName;
		private boolean m_bOpen;
		
		InputDevice(int id, String path) {
			m_nId = id; m_szPath = path; 
		}
		
		public int getPollingEvent() {
			return PollDev(m_nId);
		}

		public int getId() {
			return m_nId;
		}
		public String getName() {
			return m_szName;
		}
		public String getPath() { return m_szPath; }

		/**
		 * function Open : opens an input event node
		 * @param forceOpen will try to set permissions and then reopen if first open attempt fails
		 * @return true if input event node has been opened
		 */
		public boolean Open(boolean forceOpen) {
			int res = OpenDev(m_nId);

	   		if (res != 0) {

                ExecutorService executorService = Executors.newSingleThreadExecutor();

                Future<Integer> resultFuture = executorService.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        int res = -1;

                        if(Shell.SU.available()) {

                            Shell.SU.run("chmod 666 "+ m_szPath);
                            res = OpenDev(m_nId);
                            if(res != 0)
                            {
                                Shell.SU.run("supolicy --live " +
                                        "\"permissive appdomain\"" +
                                        "\"permissive untrusted_app\"");
                                res = OpenDev(m_nId);
                            }
                        }

                        return res;
                    }
                });

                try {
                    res = resultFuture.get();
                } catch (Exception e) {
                    res = -1;
                }
            }
	   		m_szName = getDevName(m_nId);
	   		m_bOpen = (res == 0);
	   		// debug
	   		Log.d(LT,  "Open:"+m_szPath+" Name:"+m_szName+" Result:"+m_bOpen);
	   		// done, return
	   		return m_bOpen;
	   	}
	}

    public static int getSuccessfulPollingType() {
        return getType();
    }
    public static int getSuccessfulPollingCode() {
        return getCode();
    }
    public static int getSuccessfulPollingValue() {
        return getValue();
    }
	
	// top level structures
	public ArrayList<InputDevice> m_Devs = new ArrayList<InputDevice>();

	public int Init() {
		m_Devs.clear();
		int n = ScanFiles(); // return number of devs
		if(n == -1)
		{
            ExecutorService executorService = Executors.newSingleThreadExecutor();

            Future<Integer> resultFuture = executorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int n = -1;

                    if(Shell.SU.available())
                    {
                        Shell.SU.run("supolicy --live " +
                                "\"permissive appdomain\"" +
                                "\"permissive untrusted_app\"");
                        n = ScanFiles();
                    }

                    return n;
                }
            });

            try {
                n = resultFuture.get();
            } catch (Exception e) {
                n = -1;
            }
        }
	   	
		for (int i=0;i < n;i++) 
			m_Devs.add(new InputDevice(i, getDevPath(i)));
	   	return n;
	}
	   	 
	// JNI native code interface

	private native static int ScanFiles(); // return number of devs
	private native static int OpenDev(int devid);
	private native static String getDevPath(int devid);
	private native static String getDevName(int devid);
	private native static int PollDev(int devid);
	private native static int getType();
	private native static int getCode();
	private native static int getValue();
    
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

}


