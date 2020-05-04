package com.absinthe.kage.ui.sender

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.databinding.ActivityMusicListBinding
import com.absinthe.kage.manager.GlobalManager
import com.absinthe.kage.media.audio.LocalMusic
import com.absinthe.kage.viewholder.delegate.LocalMusicViewBinder
import com.absinthe.kage.viewmodel.MusicViewModel
import com.drakeet.multitype.MultiTypeAdapter

class MusicListActivity : BaseActivity() {

    private lateinit var mBinding: ActivityMusicListBinding
    private lateinit var mViewModel: MusicViewModel
    private var mAdapter = MultiTypeAdapter()
    private var mItems = ArrayList<Any>()

    override fun setViewBinding() {
        mBinding = ActivityMusicListBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
    }

    override fun setToolbar() {
        mToolbar = mBinding.toolbar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewModel = ViewModelProvider(this).get(MusicViewModel::class.java)
        initView()
        initData()
    }

    private fun initView() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mAdapter.register(LocalMusicViewBinder())
        mBinding.rvMusicList.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(this@MusicListActivity)
        }
    }

    private fun initData() {
        mViewModel.musicList.observe(this, Observer { localMusics: MutableList<LocalMusic> ->
            mBinding.srlContainer.isRefreshing = false
            mItems.apply {
                clear()
                addAll(localMusics)
            }
            GlobalManager.musicList.apply {
                clear()
                addAll(localMusics)
            }
            mAdapter.items = mItems
            mAdapter.notifyDataSetChanged()
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
}