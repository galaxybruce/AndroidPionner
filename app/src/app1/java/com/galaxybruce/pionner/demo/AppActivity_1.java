package com.galaxybruce.pionner.demo;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.galaxybruce.pionner.R;
import com.galaxybruce.testlibrary.Lib1Activity_1;

public class AppActivity_1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_layout_1);

        findViewById(R.id.btn_lib1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AppActivity_1.this, Lib1Activity_1.class));
            }
        });
    }
}
