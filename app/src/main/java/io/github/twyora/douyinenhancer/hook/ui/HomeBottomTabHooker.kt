package io.github.twyora.douyinenhancer.hook.ui

import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.hook.core.YukiHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.HookParam
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object HomeBottomTabHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val tabIdsToRemove by lazy {
        val config = ConfigManager.homeTab
        listOf(
            config.hideHomepageHome.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_HOME,
            config.hideHomepageMallBottom.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_MALL,
            config.hideHomepageFamiliarBottom.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_FAMILIAR,
            config.hideHomepagePublish.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_PUBLISH,
            config.hideHomepageNotification.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_NOTIFICATION,
            config.hideHomepageProfile.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_PROFILE
        ).filter {
            it.first
        }.map {
            it.second
        }.toSet()
    }

    private val pruneBottomTabIds: HookParam.() -> Unit = pruneBottom@{
        val bottomTabIds = result as? MutableList<*> ?: run {
            YLog.error("$TAG: bottom tab ids is not a mutable list")
            return@pruneBottom
        }

        bottomTabIds.removeIf {
            it in tabIdsToRemove
        }
    }

    override fun onHook() {
        if (!ConfigManager.homeTab.bottomMainSwitch.value) {
            YLog.info("$TAG: bottom tab hiding disabled, skipping hook")
            return
        }

        installRemoveBottomTabItemsRemoteHook()
        installRemoveBottomTabItemsDefaultHook()
    }

    private fun installRemoveBottomTabItemsRemoteHook(): YukiHookCreator.ClassicMemberHooker.Handle? =
        packageInstance.homeTabDataSourceServer.selfClass?.resolveMethodOrNull(
            packageInstance.homeTabDataSourceServer.getShowingBottomTabIds()
        )?.hook {
            after {
                run(pruneBottomTabIds)
            }
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to remove blocklisted ids from bottom tab", throwable)
            }
        }

    private fun installRemoveBottomTabItemsDefaultHook(): YukiHookCreator.ClassicMemberHooker.Handle? =
        packageInstance.homeTabDataSourceDefault.selfClass?.resolveMethodOrNull(
            packageInstance.homeTabDataSourceDefault.getShowingBottomTabIds()
        )?.hook {
            after {
                this.run(pruneBottomTabIds)
            }
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to remove blocklisted ids from default bottom tab", throwable)
            }
        }
}
