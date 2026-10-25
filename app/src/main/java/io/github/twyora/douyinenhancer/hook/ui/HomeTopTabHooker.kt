package io.github.twyora.douyinenhancer.hook.ui

import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.hook.core.YukiHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.HookChain
import io.github.twyora.douyinenhancer.config.ConfigManager
import io.github.twyora.douyinenhancer.hook.DouyinPackage
import io.github.twyora.douyinenhancer.hook.HookOnMainProcess
import io.github.twyora.douyinenhancer.utils.reflect.resolveMethodOrNull

@HookOnMainProcess
object HomeTopTabHooker : YukiBaseHooker() {
    private val TAG = this::class.simpleName

    private val packageInstance
        get() = DouyinPackage.instance

    private val tabIdsToRemove by lazy {
        val config = ConfigManager.homeTab
        listOf(
            config.hideHomepageFollow.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_FOLLOW,
            config.hideHomepageFamiliarTop.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_FAMILIAR,
            config.hideHomepageMallTop.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_MALL,
            config.hideHomepageGroupon.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_GROUPON,
            config.hideHomepageHotContainer.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_HOT_CONTAINER,
            config.hideHomepageNearby.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_NEARBY,
            config.hideHomepageMediumvideo.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_MEDIUMVIDEO,
            config.hideHomepagePadHot.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_PAD_HOT,
            config.hideHomepageHangout.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_HANGOUT,
            config.hideHomepageTablive.value to DouyinPackage.TabNodeModule.TAB_ID_HOMEPAGE_TABLIVE
        ).filter {
            it.first
        }.map {
            it.second
        }.toSet()
    }

    private val pruneTopTabIds: HookChain.() -> Any? = pruneTop@{
        proceed<MutableList<*>>()?.apply {
            removeIf {
                it in tabIdsToRemove
            }
        } ?: run {
            YLog.error("$TAG: top tab ids is not a mutable list")
            return@pruneTop null
        }
    }

    override fun onHook() {
        if (!ConfigManager.homeTab.topMainSwitch.value) {
            YLog.info("$TAG: top tab hiding disabled, skipping hook")
            return
        }

        installRemoveTopTabItemsRemoteHook()
        installRemoveTopTabItemsDefaultHook()
    }

    private fun installRemoveTopTabItemsRemoteHook(): YukiHookCreator.MemberHooker<*>.Handle? =
        packageInstance.homeTabDataSourceServer.selfClass?.resolveMethodOrNull(
            packageInstance.homeTabDataSourceServer.getShowingTopTabIds()
        )?.intercept {
            run(pruneTopTabIds)
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to remove blocklisted ids from top tab", throwable)
            }
        }

    private fun installRemoveTopTabItemsDefaultHook(): YukiHookCreator.MemberHooker<*>.Handle? =
        packageInstance.homeTabDataSourceDefault.selfClass?.resolveMethodOrNull(
            packageInstance.homeTabDataSourceDefault.getShowingTopTabIds()
        )?.intercept {
            run(pruneTopTabIds)
        }?.apply {
            onCallbackFailure { _, throwable ->
                YLog.error("$TAG: failed to remove blocklisted ids from default top tab", throwable)
            }
        }
}
