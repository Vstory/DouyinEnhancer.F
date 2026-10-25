package io.github.twyora.douyinenhancer.hook.comment

import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.reflect.getFieldOrNull
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object CommentImageHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val verbose
        get() = !ConfigManager.module.verboseDisabled.value

    override fun onHook() {
        if (!ConfigManager.save.purifyCommentImage.value) {
            YLog.info("$TAG: purify comment image disabled, skipping hook")
            return
        }

        packageInstance.commentImageStruct.selfClass?.resolveMethodOrNull(
            packageInstance.commentImageStruct.getDownloadUrl()
        )?.intercept {
            val originUrl = instance.getFieldOrNull(
                packageInstance.commentImageStruct.originUrl()
            )?.also {
                if (verbose) {
                    YLog.debug("$TAG: origin url present, override download url with origin url")
                }
            }

            originUrl ?: proceed()
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to override download url with origin url", throwable)
            }
        }
    }
}
