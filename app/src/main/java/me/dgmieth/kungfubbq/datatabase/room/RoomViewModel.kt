package me.dgmieth.kungfubbq.datatabase.room

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import me.dgmieth.kungfubbq.datatabase.roomEntities.SocialMediaInfo
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB

enum class Actions {
    None,
    SocialMediaDelError,
    UserDelError,
    SocialMediaInsError,
    UserInsError,
    SocialMediaDeletion,
    UserDeletion,
    SocialMediaInsert,
    UserInsert
}

private const val TAG = "RoomViewModel"

class RoomViewModel:ViewModel() {
    protected val bag = CompositeDisposable()
    private var db : KungfuBBQRoomDatabase? = null

    var user = MutableLiveData<UserAndSocialMedia>()
    var returnMsg = MutableLiveData<Actions>()

    init {
        Log.d(TAG,"viewModelStarted")
    }

    fun getDbInstance(dbInstance:KungfuBBQRoomDatabase){
        this.db = dbInstance
    }
    //USER AND SOCIAL MEDIA INFO ENTITIES
    fun deleteAllUserInfo(){
        Log.d(TAG,"DeleteCalled")
        db?.kungfuBBQRoomDao()?.deleteAllUserInfo()?.observeOn(AndroidSchedulers.mainThread())?.subscribeOn(Schedulers.io())?.subscribe(
            {
                Log.d(TAG,"DeleteCalled userInfo 1")
                returnMsg.postValue(Actions.UserDeletion)
            },{
                Log.d(TAG,"DeleteCalled  userInfo  ${it}")
                returnMsg.postValue(Actions.UserDelError)
            }
        )?.let {
            bag.add(it)
        }
    }
    fun insertUserInfo(user:UserDB){
        Log.d(TAG,"InsertUserInfo called $user")
        db?.kungfuBBQRoomDao()?.insertUser(user)?.
            subscribeOn(Schedulers.io())?.
            observeOn(AndroidSchedulers.mainThread())?.
            subscribe({
                Log.d(TAG,"insertUserCalled1")
                returnMsg.postValue(Actions.UserInsert)
            },{
                Log.d(TAG,"insertUserCalled1  ${it}")
                returnMsg.postValue(Actions.UserInsError)
            })?.let{
                bag.add(it)
            }
    }
    fun deleteAllSocialMediaInfo(){
        db?.kungfuBBQRoomDao()?.deleteAllSocialMediaInfo()?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())?.subscribe(
            {
                Log.d(TAG,"DeleteCalled  socialMediaInfo 1")
                returnMsg.postValue(Actions.SocialMediaDeletion)
            },{
                Log.d(TAG,"DeleteCalled  socialMediaInfo  ${it}")
                returnMsg.postValue(Actions.SocialMediaDelError)
            }
        )?.let {
            bag.add(it)
        }
    }
    fun insertSocialMediaInfo(data:MutableList<SocialMediaInfo>){
        db?.kungfuBBQRoomDao()?.insertSocialMediaInfo(data)?.
                    subscribeOn(Schedulers.io())?.
                    observeOn(AndroidSchedulers.mainThread())?.
                    subscribe({},{})?.let{
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