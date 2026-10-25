package io.github.twyora.douyinenhancer.hook.ui

import android.app.Activity
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.ui.legacy.VerifyDialog
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object VerifyDialogHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val verbose
        get() = !ConfigManager.module.verboseDisabled.value

    override fun onHook() {
        packageInstance.mainActivity.selfClass?.resolveMethodOrNull(
            packageInstance.mainActivity.onResume()
        )?.intercept {
            val result = proceed()

            val activity = instanceOrNull<Activity>() ?: run {
                YLog.error("$TAG: ${instance::class.qualifiedName} is not an Activity")
                return@intercept result
            }
            if (VerifyDialog.shouldVerify(activity)) {
                VerifyDialog.show(activity)
            } else if (verbose) {
                YLog.debug("$TAG: no verification required")
            }

            removeSelf {
                if (verbose) {
                    YLog.debug("$TAG: verification check hook removed to prevent duplicate check")
                }
            }
            result
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to show verify dialog on resume", throwable)
            }
        }
    }
}
