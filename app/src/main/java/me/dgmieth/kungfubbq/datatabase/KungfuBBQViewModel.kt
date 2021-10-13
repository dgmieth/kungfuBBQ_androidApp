package me.dgmieth.kungfubbq.datatabase

import android.app.Application
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.log

private val TAG = "ViewModel"

class KungfuBBQViewModel (application:Application) : AndroidViewModel(application) {

    private val databaseCursor = MutableLiveData<Cursor>()
    val cursor : LiveData<Cursor>
        get() = databaseCursor

    init {
        Log.d(TAG, "viewModel initializes")
        loadTasks()
    }

    fun loadTasks(){
        val projection = arrayOf(
            UserContract.Columns.ID,
            UserContract.Columns.EMAIL,
            UserContract.Columns.NAME,
            UserContract.Columns.PHONE_NUMBER,
            UserContract.Columns.MEMBER_SINCE,
            UserContract.Columns.TOKEN,
            UserContract.Columns.LOGGED
        )
        val sortOrder = ""
        GlobalScope.launch {
            val cursor = getApplication<Application>().contentResolver.query(
                UserContract.CONTENT_URI, projection,null,null,null
            )
            databaseCursor.postValue(cursor)
        }
    }
}