package com.anna.homeworkandroidinterview.ui.main

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anna.homeworkandroidinterview.R
import com.anna.homeworkandroidinterview.core.api.NetworkService
import com.anna.homeworkandroidinterview.core.repository.ImagesRepository
import com.anna.homeworkandroidinterview.ui.adapter.ImageRecycleViewAdapter
import com.anna.homeworkandroidinterview.data.element.CardsType
import com.anna.homeworkandroidinterview.data.model.response.SearchImageResponseData
import com.anna.homeworkandroidinterview.databinding.ActivityMainBinding
import com.anna.homeworkandroidinterview.ui.AnyViewModelFactory
import com.anna.homeworkandroidinterview.ui.searchSuggest.MySuggestionProvider


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mViewModel by viewModels<MainViewModel> {
        AnyViewModelFactory{
            MainViewModel(ImagesRepository(NetworkService))
        }
    }

    private var mImageViewDataList: List<SearchImageResponseData.Info?> = listOf()

    private var mSearchView: SearchView? = null
    private val mMenuItemClick = OnMenuItemClick()
    private val mQueryTextListener = OnQueryTextListener()
    private val mSuggestionListener = OnSuggestionListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        initTopAppBar()
        initObservers()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        searchHandleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        val searchItem = menu.findItem(R.id.menu_search)
        mSearchView = searchItem.actionView as SearchView

        clearSearchHistory()
        //?????????????????????SearchView??????
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchView?.apply {
            // ??????getSearchableInfo()????????????????????????XML???????????????SearchableInfo?????????
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            // ??????????????????
            setOnQueryTextListener(mQueryTextListener)
            // ????????????????????????
            setOnSuggestionListener(mSuggestionListener)
            isSubmitButtonEnabled = true
        }
        return true
    }

    private fun initTopAppBar() {
        binding.topAppBar.setOnMenuItemClickListener(mMenuItemClick)
    }

    /**
     *  ?????? LiveData ?????????
     */
    private fun initObservers() {
        // response Success
        mViewModel.getResponseImagesList.observe(this@MainActivity) { lists ->
            mImageViewDataList = lists.dataList
            setViewLayout(binding.topAppBar.menu.findItem(R.id.menu_switch))
        }

        // response NotFound
        mViewModel.isSearchNotFound.observe(this@MainActivity) { isNotFound ->
            if (isNotFound) {
                shawDialogMessage(getString(R.string.dialog_not_found_message))
            }
        }

        // response Error
        mViewModel.responseError.observe(this@MainActivity) { errorMessage ->
            shawDialogMessage(errorMessage)
        }

        // ProgressBar
        mViewModel.isLoadRequest.observe(this@MainActivity) { isLoad ->
            if (isLoad) {
                binding.contentLoadingProgressBar.show()
            } else {
                binding.contentLoadingProgressBar.hide()
            }
        }
    }

    /**
     * ???????????????RecycleView????????????
     * Params - dataList???API????????????????????????
     *        - type???Layout?????????????????????????????????
     */
    private fun switchRecycleViewLayout(
        imageViewDataList: List<SearchImageResponseData.Info?>,
        type: CardsType
    ) {
        binding.recyclerView.adapter = ImageRecycleViewAdapter(imageViewDataList)
        when (type) {
            CardsType.GRID -> {
                binding.recyclerView.layoutManager =
                    GridLayoutManager(this, 2, RecyclerView.VERTICAL, false)
            }
            CardsType.VERTICAL -> {
                binding.recyclerView.layoutManager =
                    LinearLayoutManager(this, RecyclerView.VERTICAL, false)
            }
        }
    }

    private fun setViewLayout(menu: MenuItem) {
        when (menu.title) {
            getString(R.string.menu_item_switch_grid) -> {
                menu.title = getString(R.string.menu_item_switch_grid)
                menu.setIcon(R.drawable.ic_baseline_grid_view)
                switchRecycleViewLayout(mImageViewDataList, CardsType.VERTICAL)
            }
            getString(R.string.menu_item_switch_list) -> {
                menu.title = getString(R.string.menu_item_switch_list)
                menu.setIcon(R.drawable.ic_baseline_list_view)
                switchRecycleViewLayout(mImageViewDataList, CardsType.GRID)
            }
        }
    }

    /**
     * SearchRecentSuggestions
     * doSearchSave - ????????????????????????
     * clearSearchHistory - ??????????????????
     */
    private fun searchHandleIntent(intent: Intent) {
        //Get Intent??????????????????????????????
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                doSearchSave(query)
            }
        }
    }

    private fun doSearchSave(query: String) {
        SearchRecentSuggestions(this, MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE)
            .saveRecentQuery(query, null) // ??????1??????????????????????????????2????????????????????????????????????????????????null
    }

    private fun clearSearchHistory() {
        SearchRecentSuggestions(this, MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE)
            .clearHistory()
    }

    /**
     *  ????????????
     */
    private fun shawDialogMessage(message: String): AlertDialog {
        val builder = AlertDialog.Builder(this@MainActivity)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_positive_button) { dialog, _ ->
                dialog.dismiss()
            }

        return builder.show()
    }

    /**
     * inner class
     * OnQueryTextListener - ?????????????????????????????????
     * OnSuggestionListener - ?????????????????????????????????????????????
     * OnMenuItemClickListener - menu???????????????????????????
     */
    private inner class OnQueryTextListener : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            // ?????????API
            query?.let { mViewModel.callApiResponseData(it) }
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            return true
        }
    }

    private inner class OnSuggestionListener : SearchView.OnSuggestionListener {
        override fun onSuggestionSelect(position: Int): Boolean {
            return false
        }

        @SuppressLint("Range")
        override fun onSuggestionClick(position: Int): Boolean {
            val cursor = mSearchView?.suggestionsAdapter?.getItem(position) as Cursor
            val selection =
                cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))
            mSearchView?.setQuery(selection, false)
            return true
        }
    }

    private inner class OnMenuItemClick :
        androidx.appcompat.widget.Toolbar.OnMenuItemClickListener {
        override fun onMenuItemClick(menu: MenuItem?): Boolean {
            return when (menu?.itemId) {
                R.id.menu_switch -> {
                    when (menu.title) {
                        getString(R.string.menu_item_switch_grid) -> {
                            menu.title = getString(R.string.menu_item_switch_list)
                            menu.setIcon(R.drawable.ic_baseline_list_view)
                            switchRecycleViewLayout(mImageViewDataList, CardsType.GRID)
                        }
                        getString(R.string.menu_item_switch_list) -> {
                            menu.title = getString(R.string.menu_item_switch_grid)
                            menu.setIcon(R.drawable.ic_baseline_grid_view)
                            switchRecycleViewLayout(mImageViewDataList, CardsType.VERTICAL)
                        }
                    }
                    true
                }
                else -> {
                    false // ??????????????????
                }
            }
        }
    }

}
