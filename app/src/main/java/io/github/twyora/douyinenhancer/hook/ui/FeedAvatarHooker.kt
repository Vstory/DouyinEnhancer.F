package io.github.twyora.douyinenhancer.hook.ui

import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.hook.core.YukiHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object FeedAvatarHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val verbose
        get() = !ConfigManager.module.verboseDisabled.value

    override fun onHook() {
        if (!ConfigManager.ui.hideFollowButton.value) {
            YLog.info("$TAG: follow button hiding disabled, skipping hook")
            return
        }
        installHideFollowButtonHook()
    }

    private fun installHideFollowButtonHook(): YukiHookCreator.MemberHooker<*>.Handle? {
        return packageInstance.feedAvatarPresenter.selfClass?.resolveMethodOrNull(
            packageInstance.feedAvatarPresenter.updateFollowView()
        )?.intercept {
            val followStatusIndex = args.indexOfFirst {
                it is Int
            }.takeIf {
                it != -1
            } ?: run {
                YLog.error("$TAG: follow status not found in argument list")
                return@intercept proceed()
            }

            val followStatus = arg<Int>(followStatusIndex)

            if (followStatus != DouyinPackage.UserModule.FOLLOW_STATUS_UNFOLLOWED) {
                return@intercept proceed()
            } else if (verbose) {
                YLog.debug("$TAG: forcing unfollowed status to ${DouyinPackage.UserModule.FOLLOW_STATUS_FOLLOWING}")
            }

            proceed(
                args.toTypedArray().apply {
                    set(followStatusIndex, DouyinPackage.UserModule.FOLLOW_STATUS_FOLLOWING)
                }
            )
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to hide follow button", throwable)
            }
        }
    }
}
