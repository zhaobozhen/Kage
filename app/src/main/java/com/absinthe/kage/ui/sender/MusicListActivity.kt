package com.absinthe.kage.ui.sender

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.adapter.MusicListAdapter
import com.absinthe.kage.databinding.ActivityMusicListBinding
import com.absinthe.kage.media.audio.LocalMusic
import com.absinthe.kage.ui.media.MusicActivity
import com.absinthe.kage.viewmodel.MusicViewModel
import com.chad.library.adapter.base.BaseQuickAdapter
import java.util.*

class MusicListActivity : BaseActivity() {

    private lateinit var mBinding: ActivityMusicListBinding
    private var mViewModel: MusicViewModel = ViewModelProvider(this).get(MusicViewModel::class.java)
    private var mAdapter: MusicListAdapter = MusicListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMusicListBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initView()
        initData()
    }

    private fun initView() {
        mBinding.rvMusicList.adapter = mAdapter
        mBinding.rvMusicList.layoutManager = LinearLayoutManager(this)
        mAdapter.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>, _: View?, position: Int ->
            val localMusic = adapter.data[position] as LocalMusic?
            if (localMusic != null) {
                val intent = Intent(this@MusicListActivity, MusicActivity::class.java)
                intent.putExtra(MusicActivity.EXTRA_MUSIC_INFO, localMusic)
                intent.putExtra(MusicActivity.EXTRA_DEVICE_TYPE, MusicActivity.TYPE_SENDER)
                startActivity(intent)
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initData() {
        mViewModel.musicList.observe(this, Observer { localMusics: MutableList<LocalMusic> ->
            mBinding.srlContainer.isRefreshing = false
            mAdapter.setNewData(localMusics)
            sMusicList = localMusics
            mBinding.srlContainer.isEnabled = false
        })

        mBinding.srlContainer.isRefreshing = true
        mViewModel.loadMusic(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmField
        var sMusicList: List<LocalMusic> = ArrayList()
    }
}