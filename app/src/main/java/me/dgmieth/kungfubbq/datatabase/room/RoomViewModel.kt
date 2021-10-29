package me.dgmieth.kungfubbq.datatabase.room

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import me.dgmieth.kungfubbq.datatabase.roomEntities.*

enum class Actions {
    None,
    SocialMediaDelError,
    UserDelError,
    SocialMediaInsError,
    UserInsError,
    SocialMediaDeletion,
    UserDeletion,
    SocialMediaInsert,
    UserInsert,
    UserComplete,
    UserError,
    CookingDatesComplete,
    CookingDatesError;
}

private const val TAG = "RoomViewModel"

class RoomViewModel:ViewModel() {
    protected val bag = CompositeDisposable()
    private var db : KungfuBBQRoomDatabase? = null

    var user = MutableLiveData<UserAndSocialMedia>()
    var order = MutableLiveData<CookingDateAndCookingDateDishesWithOrder>()
    var returnMsg : PublishSubject<Actions> = PublishSubject.create()


    init {
        Log.d(TAG,"viewModelStarted")
    }

    fun getDbInstance(dbInstance:KungfuBBQRoomDatabase){
        this.db = dbInstance
    }
    //USER AND SOCIAL MEDIA INFO ENTITIES
//    fun deleteAllUserInfo(){
//        Log.d(TAG,"DeleteCalled")
//        db?.kungfuBBQRoomDao()?.deleteAllUserInfo()?.observeOn(AndroidSchedulers.mainThread())?.subscribeOn(Schedulers.io())?.subscribe(
//            {
//                Log.d(TAG,"DeleteCalled userInfo 1")
//                returnMsg.onNext(Actions.UserDeletion)
//            },{
//                Log.d(TAG,"DeleteCalled  userInfo  ${it}")
//                returnMsg.onNext(Actions.UserDelError)
//            }
//        )?.let {
//            bag.add(it)
//        }
//    }
    fun insertAllUserInfo(user:UserDB,socialMediaList:MutableList<SocialMediaInfo>){
        db?.kungfuBBQRoomDao()?.deleteAllUserInfo()?.observeOn(AndroidSchedulers.mainThread())?.subscribeOn(Schedulers.io())?.
        subscribe({
            Log.d(TAG,"DeleteCalled userInfo 1")
            db?.kungfuBBQRoomDao()?.deleteAllSocialMediaInfo()?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())?.
            subscribe({
                Log.d(TAG,"DeleteCalled  socialMediaInfo 1")
                db?.kungfuBBQRoomDao()?.insertUser(user)?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())?.
                subscribe({
                    Log.d(TAG,"insertUserCalled1")
                    db?.kungfuBBQRoomDao()?.insertSocialMediaInfo(socialMediaList)?.
                    subscribeOn(Schedulers.io())?.
                    observeOn(AndroidSchedulers.mainThread())?.
                    subscribe({
                        returnMsg.onNext(Actions.UserComplete)
                    },{
                        returnMsg.onNext(Actions.UserError)
                    })?.let{
                        bag.add(it)
                    }
                },{
                    Log.d(TAG,"insertUserCalled1  ${it}")
                    returnMsg.onNext(Actions.UserError)
                })?.let{
                    bag.add(it)
                }
                },{
                    Log.d(TAG,"DeleteCalled  socialMediaInfo  ${it}")
                    returnMsg.onNext(Actions.UserError)
                }
            )?.let {
                bag.add(it)
            }
            },{
                Log.d(TAG,"DeleteCalled  userInfo  ${it}")
                returnMsg.onNext(Actions.UserError)
            }
        )?.let {
            bag.add(it)
        }
    }
//    fun insertUserInfo(user:UserDB){
//        Log.d(TAG,"InsertUserInfo called $user")
//        db?.kungfuBBQRoomDao()?.insertUser(user)?.
//            subscribeOn(Schedulers.io())?.
//            observeOn(AndroidSchedulers.mainThread())?.
//            subscribe({
//                Log.d(TAG,"insertUserCalled1")
//                returnMsg.onNext(Actions.UserInsert)
//            },{
//                Log.d(TAG,"insertUserCalled1  ${it}")
//                returnMsg.onNext(Actions.UserInsError)
//            })?.let{
//                bag.add(it)
//            }
//    }
//    fun deleteAllSocialMediaInfo(){
//        db?.kungfuBBQRoomDao()?.deleteAllSocialMediaInfo()?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())?.subscribe(
//            {
//                Log.d(TAG,"DeleteCalled  socialMediaInfo 1")
//                returnMsg.onNext(Actions.SocialMediaDeletion)
//            },{
//                Log.d(TAG,"DeleteCalled  socialMediaInfo  ${it}")
//                returnMsg.onNext(Actions.SocialMediaDelError)
//            }
//        )?.let {
//            bag.add(it)
//        }
//    }
//    fun insertSocialMediaInfo(data:MutableList<SocialMediaInfo>){
//        db?.kungfuBBQRoomDao()?.insertSocialMediaInfo(data)?.
//                    subscribeOn(Schedulers.io())?.
//                    observeOn(AndroidSchedulers.mainThread())?.
//                    subscribe({
//                        returnMsg.onNext(Actions.SocialMediaInsert)
//                    },{
//                        returnMsg.onNext(Actions.SocialMediaInsError)
//                    })?.let{
//                        bag.add(it)
//        }
//    }
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
    /* ==================================================================================
    COOKING DATE, DISHES and ORDERS
       ==================================================================================*/
    fun insertAllCookingDates(cookingDates: MutableList<CookingDateDB>,cookingDatesDishes: MutableList<CookingDateDishesDB>,orders:MutableList<OrderDB>,orderDishes:MutableList<OrderDishesDB>){
        Log.d(TAG,"insertCookingDates called $cookingDates")
        db?.kungfuBBQRoomDao()?.deleteAllCookingDates()
            ?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                Log.d(TAG,"cookingDates delete")
                db?.kungfuBBQRoomDao()?.deleteAllCookingDateDishes()?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                        Log.d(TAG,"cookingDatesDishes delete")
                        db?.kungfuBBQRoomDao()?.deleteAllOrders()?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())
                            ?.subscribe({
                                Log.d(TAG,"orders delete")
                                db?.kungfuBBQRoomDao()?.insertAllCookingDates(cookingDates)?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())
                                    ?.subscribe({
                                        Log.d(TAG,"cookingDates insert")
                                        Log.d(TAG,"orders delete")
                                        db?.kungfuBBQRoomDao()?.insertAllCookingDatesDishes(cookingDatesDishes)?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())
                                            ?.subscribe({
                                                Log.d(TAG,"cookingDatesDishes insert")
                                                db?.kungfuBBQRoomDao()?.insertAllOrders(orders)?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())
                                                    ?.subscribe({
                                                        Log.d(TAG,"orders insert")
                                                        db?.kungfuBBQRoomDao()?.insertAllOrdersDishes(orderDishes)?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())
                                                            ?.subscribe({
                                                                Log.d(TAG,"orders insert")
                                                                returnMsg.onNext(Actions.CookingDatesComplete)
                                                            },{
                                                                Log.d(TAG,"orders insertion error $it")
                                                                returnMsg.onNext(Actions.CookingDatesComplete)
                                                            })?.let {
                                                                bag.add(it)
                                                            }
                                                    },{
                                                        Log.d(TAG,"orders insertion error $it")
                                                        returnMsg.onNext(Actions.CookingDatesComplete)
                                                    })?.let {
                                                        bag.add(it)
                                                    }
                                            },{
                                                Log.d(TAG,"cookingDatesDishes insertion error $it")
                                                returnMsg.onNext(Actions.CookingDatesComplete)
                                            })?.let {
                                                bag.add(it)
                                            }
                                    },{
                                        Log.d(TAG,"cookingDates insertion error $it")
                                        returnMsg.onNext(Actions.CookingDatesComplete)
                                    })?.let {
                                        bag.add(it)
                                    }
                            },{
                                Log.d(TAG,"orders insertion error $it")
                            })?.let {
                                bag.add(it)
                            }
                    },{
                        Log.d(TAG,"cookingDatesDishes insertion error $it")
                    })?.let {
                        bag.add(it)
                    }
            },{
                Log.d(TAG,"cookingDates insertion error $it")
            })?.let {
                bag.add(it)
            }

        db?.kungfuBBQRoomDao()?.insertAllCookingDates(cookingDates)
    }
    fun getCookingDate(cookingDateId:Int){
        Log.d(TAG,"getOrder called $cookingDateId")
        db?.kungfuBBQRoomDao()?.getCookingDate(cookingDateId)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                Log.d(TAG,"getOrder called ${it}")
                        order.postValue(it)
            },{
                Log.d(TAG,"getOrder called error ${it}")
            })?.let{
                bag.add(it)
            }
    }
    override fun onCleared(){
        bag.dispose()
        bag.clear()
        super.onCleared()
    }
}