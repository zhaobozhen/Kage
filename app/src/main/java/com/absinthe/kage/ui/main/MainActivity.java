package com.absinthe.kage.ui.main;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.R;
import com.absinthe.kage.server.ConnectionServer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ConnectionServer connectionServer = new ConnectionServer();

        Button btnS = findViewById(R.id.btn_s);
        Button btnC = findViewById(R.id.btn_c);

        btnS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectionServer.start();
            }
        });
    }
}
