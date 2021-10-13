package me.dgmieth.kungfubbq.datatabase

open class SingletonCreator<out T: Any,in A>(creator: (A)->T){
    private var creator : ((A)->T)? = creator
    @Volatile private var instance: T? = null

    fun getInstace(arg: A) : T {
        val checkInstance = instance
        if(checkInstance!=null){
            return checkInstance
        }
        return synchronized(this){
            val checkInstanceAgain = instance
            if(checkInstanceAgain!=null){
                checkInstanceAgain
            }else{
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}