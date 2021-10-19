package me.dgmieth.kungfubbq.datatabase.room

import android.nfc.Tag
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB

private const val TAG = "RoomViewModel"

class RoomViewModel:ViewModel() {
    protected val bag = CompositeDisposable()
    private var db : KungfuBBQRoomDatabase? = null

    var user = MutableLiveData<UserAndSocialMedia>()

    init {
        Log.d(TAG,"viewModelStarted")
    }

    fun getDbInstance(dbInstance:KungfuBBQRoomDatabase){
        this.db = dbInstance
    }

    fun insertUserInfo(user:UserDB){
        db?.kungfuBBQRoomDao()?.insertUser(user)?.
            subscribeOn(Schedulers.io())?.
            observeOn(AndroidSchedulers.mainThread())?.
            subscribe({

            },{

            })?.let{
                bag.add(it)
            }
    }
    fun getUser(){
        db?.kungfuBBQRoomDao()?.getUser()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                          if(!it.user.email.isNullOrEmpty()){
                              user.postValue(it)
                              Log.d(TAG, "userReturned with values $user")
                          }
                },{})?.let{
                    bag.add(it)
        }
    }

    override fun onCleared(){
        bag.dispose()
        bag.clear()
        super.onCleared()
    }
}