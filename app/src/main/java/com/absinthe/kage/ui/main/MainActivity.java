package com.absinthe.kage.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.R;
import com.absinthe.kage.databinding.ActivityMainBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.service.TCPService;
import com.absinthe.kage.ui.about.AboutActivity;
import com.absinthe.kage.ui.connect.ConnectActivity;
import com.absinthe.kage.ui.sender.SenderActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();

        TCPService.start(this);
    }

    @Override
    protected void onDestroy() {
        TCPService.stop(this);
        super.onDestroy();
    }

    private void initView() {
        binding.btnSend.setOnClickListener(v -> {
            if (DeviceManager.Singleton.INSTANCE.getInstance().isConnected()) {
                startActivity(new Intent(MainActivity.this, SenderActivity.class));
            } else {
                startActivity(new Intent(MainActivity.this, ConnectActivity.class));
            }
        });
        binding.btnConnect.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ConnectActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.toolbar_about) {
            startActivity(new Intent(this, AboutActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
