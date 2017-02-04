package com.tushar.cmspen2;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.tushar.cmspen2.libsuperuser.Shell;

public class KeyboardActivity extends Activity {
	
	ArrayList<String> list;
    String currentKB = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_keyboard);
		GridView grid = (GridView) findViewById(R.id.kb_list);
		currentKB = getIntent().getStringExtra("c_kb");
		list = new ArrayList<String>();
		
		String result = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_INPUT_METHODS);
		for (String retval: result.split(":"))
		{
			list.add(retval);
		}
		        	
		ListAdapter adapter = new ListAdapter(list);
		
		grid.setAdapter(adapter);
		
		grid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				//SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(KeyboardActivity.this);
				//SharedPreferences.Editor edit = pref.edit();
				//edit.putString("s_kb", list.get(arg2));
				//edit.apply();
                TextView title = (TextView) arg1.findViewById(R.id.selector_title);
                Intent result = new Intent();
                result.putExtra("s_kb", list.get(arg2));
                result.putExtra("name", title.getText().toString());
                KeyboardActivity.this.setResult(Activity.RESULT_OK, result);
				finish();
			}
			
		});
	}
	
	class ListAdapter extends ArrayAdapter<String> {
	    
	    ListAdapter(List<String> apps) {
	    	super(KeyboardActivity.this, R.layout.selector_item, apps);
	    }
	    
	    @Override
	    public View getView(int position, View convertView,
	                          ViewGroup parent) {
	    	if (convertView==null) {
	    		convertView=newView(parent);
	    	}
	      
	    	bindView(position, convertView);
	      
	    	return(convertView);
	    }
	    
	    private View newView(ViewGroup parent) {
	    	return(getLayoutInflater().inflate(R.layout.selector_item, parent, false));
	    }
	    
	    private void bindView(int position, View row) {
	    	
	    	PackageManager pm = KeyboardActivity.this.getPackageManager();
	    	String c_item = list.get(position);
            if(currentKB != null)
                if(c_item.equals(currentKB))
                    row.setBackgroundColor(Color.argb(170, 51, 181, 229));
	    	String pkg = c_item.substring(0, c_item.indexOf('/'));
	    	ApplicationInfo ai;
	    	try
	    	{
	    		ai = pm.getApplicationInfo(pkg, 0);
	    		TextView label=(TextView)row.findViewById(R.id.selector_title);
		    	label.setText(pm.getApplicationLabel(ai));
		    	ImageView icon=(ImageView)row.findViewById(R.id.selector_icon);
		    	icon.setImageDrawable(pm.getApplicationIcon(ai));
	    	}
	    	catch(Exception e) {}
	      
	    }
	  }

    public static void Switch(final String id, final String switchTo, Context ctx)
    {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor edit = pref.edit();

        final String[] c_kb = {Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD)};

		new Thread() {
            @Override
            public void run() {

                if(!switchTo.equals("") && Shell.SU.available())
                {
                    if(!switchTo.equals(c_kb[0]))
                    {
                        Shell.SU.run("ime set " + switchTo);
                        edit.putString(id + "c_kb", c_kb[0]);
                        edit.apply();
                    }
                    else
                    {
                        c_kb[0] = pref.getString(id + "c_kb", "");
                        if(!c_kb[0].equals(""))
                        {
                            Shell.SU.run("ime set " + c_kb[0]);
                        }
                        edit.putString(id + "c_kb", "");
                        edit.apply();
                    }
                }
            }
        }.start();
    }
}
