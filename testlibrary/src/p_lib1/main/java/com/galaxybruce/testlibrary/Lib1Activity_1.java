package com.galaxybruce.testlibrary;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

/**
 * @date 2020/11/15 17:42
 * @author bruce.zhang
 * @description (亲，我是做什么的)
 * <p>
 * modification history:
 */
public class Lib1Activity_1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lib1_layout);

        findViewById(R.id.btn_lib1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Lib1Activity_1.this, Lib1Activity_2.class));
            }
        });
    }
}
