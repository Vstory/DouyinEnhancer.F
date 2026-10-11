package io.github.twyora.douyinenhancer.hook.ui

import com.highcapable.yukihookapi.hook.core.YukiMemberHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.resolveMethodOrNull

@HookOnMainProcess
object FeedAvatarHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val verbose
        get() = ConfigManager.module.verboseDisabled.value

    override fun onHook() {
        if (!ConfigManager.ui.hideFollowButton.value) {
            YLog.info("$TAG: follow button hiding disabled, skipping hook")
            return
        }
        installHideFollowButtonHook()
    }

    private fun installHideFollowButtonHook(): YukiMemberHookCreator.MemberHookCreator.Result? {
        return packageInstance.feedAvatarPresenter.selfClass?.resolveMethodOrNull(
            packageInstance.feedAvatarPresenter.updateFollowView()
        )?.hook {
            before {
                val followStatusIndex = args.indexOfFirst {
                    it is Int
                }.takeIf {
                    it != -1
                } ?: run {
                    YLog.error("$TAG: follow status not found in argument list")
                    return@before
                }

                val followStatus = args[followStatusIndex] as Int

                if (followStatus != DouyinPackage.UserModule.FOLLOW_STATUS_UNFOLLOWED) {
                    return@before
                } else if (verbose) {
                    YLog.debug("$TAG: forcing unfollowed status to ${DouyinPackage.UserModule.FOLLOW_STATUS_FOLLOWING}")
                }

                args[followStatusIndex] = DouyinPackage.UserModule.FOLLOW_STATUS_FOLLOWING
            }
        }?.result {
            onConductFailure { _, throwable ->
                YLog.error("$TAG: failed to hide follow button", throwable)
            }
            onHookingFailure { throwable ->
                YLog.error("$TAG: failed to hook follow button hiding", throwable)
            }
        }
    }
}
