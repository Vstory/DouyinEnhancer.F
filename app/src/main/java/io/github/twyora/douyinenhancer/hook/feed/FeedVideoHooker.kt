package io.github.twyora.douyinenhancer.hook.feed

import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.reflect.invokeMethodOrNull
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object FeedVideoHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val verbose
        get() = !ConfigManager.module.verboseDisabled.value

    override fun onHook() {
        if (!ConfigManager.save.feedVideoRemoveWatermark.value) {
            YLog.info("$TAG: remove watermark is disabled, skipping hook")
            return
        }

        packageInstance.miscDownloadAddrUtil.selfClass?.resolveMethodOrNull(
            packageInstance.miscDownloadAddrUtil.getSuffixSceneDownloadAddr()
        )?.intercept {
            val aweme = args.firstOrNull {
                packageInstance.aweme.selfClass?.isInstance(it) == true
            } ?: run {
                YLog.error("$TAG: aweme not found in argument list")
                return@intercept proceed()
            }

            aweme.invokeMethodOrNull(
                packageInstance.aweme.getVideo()
            )?.invokeMethodOrNull(
                packageInstance.video.getPlayAddr()
            )?.also {
                if (verbose) {
                    YLog.debug("$TAG: play addr present, override result with play addr")
                }
            } ?: proceed()
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to override download addr with play addr", throwable)
            }
        }
    }
}
