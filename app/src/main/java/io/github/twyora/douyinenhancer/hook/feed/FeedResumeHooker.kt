package io.github.twyora.douyinenhancer.hook.feed

import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.reflect.getFieldOrNull
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object FeedResumeHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val verbose
        get() = !ConfigManager.module.verboseDisabled.value

    override fun onHook() {
        if (!ConfigManager.feed.blockResumePlayback.value) {
            YLog.info("$TAG: block resume playback is disabled, skipping hook")
            return
        }

        packageInstance.baseListFragmentPanel.selfClass?.resolveMethodOrNull(
            packageInstance.baseListFragmentPanel.canPlayOnVideoReady()
        )?.intercept {
            val playStatus = instance.getFieldOrNull<Any>(
                packageInstance.baseListFragmentPanel.mPlayStateHelper()
            )?.getFieldOrNull<Int>(
                packageInstance.playStatusHelper.playStatus()
            ) ?: DouyinPackage.PlayStatusHelperModule.PLAY_STATUS_PAUSED

            if (playStatus != DouyinPackage.PlayStatusHelperModule.PLAY_STATUS_PAUSED) {
                return@intercept proceed()
            } else if (verbose) {
                YLog.debug("$TAG: current play status is paused; intercept to prevent resume")
            }

            false
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to intercept resume of video playback on surface created", throwable)
            }
        }
    }
}
