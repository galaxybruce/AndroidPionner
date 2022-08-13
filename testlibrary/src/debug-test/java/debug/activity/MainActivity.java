package debug.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.galaxybruce.testlibrary.Lib1Activity_1;
import com.galaxybruce.testlibrary.R;

import androidx.appcompat.app.AppCompatActivity;

/**
 * @date 2020/11/15 17:42
 * @author bruce.zhang
 * @description (亲，我是做什么的)
 * <p>
 * modification history:
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        findViewById(R.id.btn_lib1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Lib1Activity_1.class));
            }
        });
    }
}
