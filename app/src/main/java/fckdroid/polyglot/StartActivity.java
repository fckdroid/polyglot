package fckdroid.polyglot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        findViewById(R.id.start_btn_exit).setOnClickListener(v -> finish());
        findViewById(R.id.start_btn_play).setOnClickListener(v -> {
            Intent toGame = new Intent(this, GameActivity.class);
            startActivity(toGame);
        });
    }


}
