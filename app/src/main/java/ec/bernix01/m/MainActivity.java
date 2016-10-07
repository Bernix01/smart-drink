package ec.bernix01.m;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.github.anastr.speedviewlib.SpeedView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpeedView speedView = (SpeedView) findViewById(R.id.speedView);
        speedView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), android.R.color.transparent));
        speedView.setMaxSpeed(5);
        speedView.speedPercentTo(33);

    }
}
