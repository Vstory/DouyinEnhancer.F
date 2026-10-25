package io.github.twyora.douyinenhancer.hook.feed

import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.hook.core.YukiHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.reflect.getFieldOrNull
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object ListenAwemeFilterHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val verbose
        get() = !ConfigManager.module.verboseDisabled.value

    override fun onHook() {
        if (!ConfigManager.feed.bypassListenAwemeRestriction.value) {
            YLog.info("$TAG: bypass listen aweme restriction is disabled, skipping hook")
            return
        }
        installBypassListenAwemeFilterHook()
    }

    private fun installBypassListenAwemeFilterHook(): YukiHookCreator.MemberHooker<*>.Handle? {
        return packageInstance.listenAwemeFilter.selfClass?.resolveMethodOrNull(
            packageInstance.listenAwemeFilter.accept()
        )?.intercept {
            val result = proceed<Boolean>()
            if (result == true) {
                return@intercept result
            }

            if (verbose) {
                val awemeId = args.firstOrNull {
                    packageInstance.aweme.selfClass?.isInstance(it) == true
                }?.getFieldOrNull<String>(
                    packageInstance.aweme.aid()
                )
                YLog.debug("$TAG: bypassing listen aweme filter for aweme id: $awemeId")
            }
            true
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to bypass aweme in filter", throwable)
            }
        }
    }
}
