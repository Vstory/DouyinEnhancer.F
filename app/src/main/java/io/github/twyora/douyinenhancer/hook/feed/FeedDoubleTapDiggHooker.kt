package io.github.twyora.douyinenhancer.hook.feed

import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object FeedDoubleTapDiggHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val verbose
        get() = !ConfigManager.module.verboseDisabled.value

    override fun onHook() {
        if (!ConfigManager.feed.interceptDoubleTapDigg.value) {
            YLog.info("$TAG: double-tap digg interception is disabled, skipping hook")
            return
        }

        packageInstance.baseListFragmentPanel.selfClass?.resolveMethodOrNull(
            packageInstance.baseListFragmentPanel.handleBigDiggViewClick()
        )?.intercept {
            if (verbose) {
                YLog.debug("$TAG: intercepting double-tap digg")
            }
            null
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to intercept double-tap digg", throwable)
            }
        }
    }
}
