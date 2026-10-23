package io.github.twyora.douyinenhancer.hook.ui

import android.view.View
import androidx.collection.ArraySet
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.hook.core.YukiHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object DanmakuViewHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val verbose
        get() = !ConfigManager.module.verboseDisabled.value

    private val danmakuViewIds = ArraySet<Int>().apply {
        add(View.generateViewId())
    }

    override fun onHook() {
        if (!ConfigManager.ui.keepDanmakuVisible.value) {
            YLog.info("$TAG: keep danmaku visible disabled, skipping hook")
            return
        }
        installAssignDanmakuViewIdHook()
        installBlockDanmakuContainerVisibilityChangeHook()
    }

    private fun installAssignDanmakuViewIdHook(): YukiHookCreator.ClassicMemberHooker.Handle? {
        return packageInstance.danmakuView.selfClass?.resolveMethodOrNull(
            packageInstance.danmakuView.onAttachedToWindow()
        )?.hook {
            after {
                val danmakuView = instanceOrNull<View>() ?: run {
                    YLog.error("$TAG: ${instance::class.qualifiedName} is not View")
                    return@after
                }

                if (danmakuView.id == View.NO_ID) {
                    val danmakuViewId = danmakuViewIds.first()
                    if (verbose) {
                        YLog.debug("$TAG: assign danmaku view id($danmakuViewId) to view without id")
                    }
                    danmakuView.id = danmakuViewId
                } else {
                    if (verbose) {
                        YLog.debug("$TAG: collect existing danmaku view id(${danmakuView.id})")
                    }
                    danmakuViewIds.add(danmakuView.id)
                }
            }
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to assign danmaku view id", throwable)
            }
        }
    }

    private fun installBlockDanmakuContainerVisibilityChangeHook(): YukiHookCreator.ClassicMemberHooker.Handle? {
        return packageInstance.cleanModePresenter.selfClass?.resolveMethodOrNull(
            packageInstance.cleanModePresenter.manageViewVisibility()
        )?.hook {
            before {
                val managedView = args.firstNotNullOfOrNull {
                    it as? View
                } ?: run {
                    YLog.error("$TAG: managed view not found in argument list")
                    return@before
                }

                if (danmakuViewIds.none {
                        managedView.findViewById<View?>(it) != null
                    }
                ) {
                    return@before
                }

                YLog.info(
                    "$TAG: ${managedView::class.qualifiedName}{id=0x${managedView.id.toString(16)}} is" +
                        " a danmaku view container; will intercept to block visibility state change"
                )
                result = null
            }
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to block danmaku container visibility change", throwable)
            }
        }
    }
}
