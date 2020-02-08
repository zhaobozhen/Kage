package com.absinthe.kage.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.R;
import com.absinthe.kage.databinding.ActivityMainBinding;
import com.absinthe.kage.service.TCPService;
import com.absinthe.kage.ui.connect.ConnectActivity;
import com.absinthe.kage.ui.receiver.ReceiverActivity;
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
        binding.btnSender.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SenderActivity.class)));
        binding.btnReceiver.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ReceiverActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.toolbar_connect) {
            startActivity(new Intent(this, ConnectActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
