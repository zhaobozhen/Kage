package com.absinthe.kage.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.client.ConnectionClient;
import com.absinthe.kage.client.IRequestCallBack;
import com.absinthe.kage.databinding.ActivityMainBinding;
import com.absinthe.kage.protocol.BaseProtocol;
import com.absinthe.kage.protocol.DataProtocol;
import com.absinthe.kage.service.TCPService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private ConnectionClient mClient;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        startService(new Intent(this, TCPService.class));
        binding.btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClient = new ConnectionClient(new IRequestCallBack() {
                    @Override
                    public void onSuccess(BaseProtocol msg) {
                        Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(int errorCode, String msg) {
                        Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        binding.btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataProtocol dataProtocol = new DataProtocol();
                dataProtocol.setDtype(1);
                dataProtocol.setPattion(2);
                dataProtocol.setMsgId(count++);
                dataProtocol.setData("Send data");
                mClient.addNewRequest(dataProtocol);
            }
        });
    }

    @Override
    protected void onDestroy() {
        mClient.closeConnect();
        super.onDestroy();
    }
}
