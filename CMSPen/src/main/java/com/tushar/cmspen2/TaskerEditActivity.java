package com.tushar.cmspen2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;


public class TaskerEditActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasker_edit);
        final RadioGroup list = (RadioGroup) findViewById(R.id.tasker_list);

        Button save = (Button) findViewById(R.id.tasker_save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = list.getCheckedRadioButtonId();
                if(id != -1) {
                    int selected = -1;
                    String text = "";
                    switch (id) {
                        case R.id.t_single_press:
                            selected = 1;
                            text = "Single Click";
                            break;
                        case R.id.t_long_press:
                            selected = 2;
                            text = "Long Click";
                            break;
                        case R.id.t_double_press:
                            selected = 3;
                            text = "Double Click";
                    }
                    Bundle result = new Bundle();
                    result.putInt("com.tushar.cmspen.BUTTON_EVENT_SELECTION", selected);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", result);
                    resultIntent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", text);

                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
                else
                    Toast.makeText(TaskerEditActivity.this, "Invalid Selection!", Toast.LENGTH_LONG).show();
            }
        });

        Button cancel = (Button) findViewById(R.id.tasker_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
}
