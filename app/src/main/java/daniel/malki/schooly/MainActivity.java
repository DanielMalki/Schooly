package daniel.malki.schooly;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends BaseMenuActivity {

    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Home");

        button = findViewById(R.id.button);
        button.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class))
        );
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }
}
