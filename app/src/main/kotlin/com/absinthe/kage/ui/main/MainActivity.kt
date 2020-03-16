package com.absinthe.kage.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.R
import com.absinthe.kage.databinding.ActivityMainBinding
import com.absinthe.kage.service.TCPService
import com.absinthe.kage.ui.about.AboutActivity
import com.absinthe.kage.ui.connect.ConnectActivity
import com.absinthe.kage.ui.sender.SenderActivity
import com.blankj.utilcode.util.ServiceUtils

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()

        if (!ServiceUtils.isServiceRunning(TCPService::class.java)) {
            TCPService.start(this)
        }
    }

    override fun onDestroy() {
        TCPService.stop(this)
        super.onDestroy()
    }

    private fun initView() {
        binding.btnSend.setOnClickListener {
            startActivity(Intent(this@MainActivity, SenderActivity::class.java))
        }
        binding.btnConnect.setOnClickListener {
            startActivity(Intent(this@MainActivity, ConnectActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_about) {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }
}