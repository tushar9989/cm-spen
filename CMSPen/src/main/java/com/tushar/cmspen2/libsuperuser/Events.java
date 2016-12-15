package com.tushar.cmspen2.libsuperuser;

import java.util.ArrayList;

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
	   		// if opening fails, we might not have the correct permissions, try changing 660 to 666
	   		if (res != 0) {
	   			// possible only if we have root
	   			if(forceOpen && Shell.SU.available()) {
	   				// set new permissions
	   				Shell.SU.run("chmod 666 "+ m_szPath);
	   				// reopen
	   			    res = OpenDev(m_nId);
                    if(res != 0)
                    {
						/*Shell.SU.run("supolicy --live " +
								"\"allow appdomain input_device dir { ioctl read getattr search open }\" " +
								"\"allow appdomain input_device chr_file { ioctl read write getattr lock append open }\" " +
								"\"allow untrusted_app input_device chr_file { ioctl read write getattr lock append open }\" " +
								"\"allow untrusted_app input_device dir { ioctl read getattr search open }\"");*/
                        //Shell.SU.run("supolicy --live " +
                                //"\"allow fuse tmpfs filesystem { associate }\"");
                        //Shell.SU.run("chcon u:object_r:fuse:s0 /dev/input/*");
						//Shell.SU.run("supolicy --live \"permissive input_device\"");
                        Shell.SU.run("su --context u:r:init:s0 -c \"chcon u:object_r:fuse:s0 /dev/input/*\"");
                        res = OpenDev(m_nId);
                        /*if(res != 0)
                        {
                            Shell.runCommand("setenforce 0");
                            res = OpenDev(m_nId);
                        }*/
                    }
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
	
	/*private void displayError()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this,android.R.style.Theme_DeviceDefault));
        builder.setTitle("Screenshot failed");
        builder.setMessage("Please install SuperSU from the Play Store and try again.");
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //finish();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }*/

	public int Init() {
		m_Devs.clear();
		int n = ScanFiles(); // return number of devs
		if(n == -1)
		{
			if(Shell.SU.available())
            {
				/*Shell.SU.run("supolicy --live " +
						"\"allow appdomain input_device dir { ioctl read getattr search open }\" " +
						"\"allow appdomain input_device chr_file { ioctl read write getattr lock append open }\" " +
						"\"allow untrusted_app input_device chr_file { ioctl read write getattr lock append open }\" " +
						"\"allow untrusted_app input_device dir { ioctl read getattr search open }\"");*/
                //Shell.SU.run("supolicy --live " +
                        //"\"allow fuse tmpfs filesystem { associate }\"");
                Shell.SU.run("su --context u:r:init:s0 -c \"chcon u:object_r:fuse:s0 /dev/input/*\"");
				//Shell.SU.run("supolicy --live \"permissive input_device\"");
                n = ScanFiles();
                //if(n == -1)
                //{
                    //displayError();
                    //Shell.runCommand("setenforce 0");
                    //n = ScanFiles();
                //}
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


