package io.github.twyora.douyinenhancer.bridge

import android.content.res.Resources
import com.highcapable.yukihookapi.hook.param.PackageParam

class ModuleApp private constructor(val resources: Resources) {
    companion object {
        @Volatile
        lateinit var instance: ModuleApp
            private set

        fun init(packageParam: PackageParam) {
            instance = ModuleApp(packageParam.module.resources)
        }
    }
}
