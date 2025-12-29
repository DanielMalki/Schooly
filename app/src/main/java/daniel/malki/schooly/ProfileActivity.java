package daniel.malki.schooly;

import android.os.Bundle;

public class ProfileActivity extends BaseMenuActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Profile");
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_profile;
    }
}
