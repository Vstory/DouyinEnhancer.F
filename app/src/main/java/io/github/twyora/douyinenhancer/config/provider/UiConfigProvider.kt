package io.github.twyora.douyinenhancer.config.provider

import io.github.twyora.douyinenhancer.config.kvstorage.IKVStorage
import io.github.twyora.douyinenhancer.config.rule.IRule

class UiConfigProvider(
    kvConfig: IKVStorage,
    ruleContextProvider: () -> IRule.Context = {
        IRule.Context(hiddenFeatureEnabled = true)
    }
) : AbsConfigProvider(kvConfig, ruleContextProvider) {
    val keepDanmakuVisible = configItem(KEEP_DANMAKU_VISIBLE, false)
    val hideFollowButton = configItem(HIDE_FOLLOW_BUTTON, false)

    companion object {
        const val KEEP_DANMAKU_VISIBLE = "keep_danmaku_visible"
        const val HIDE_FOLLOW_BUTTON = "hide_follow_button"
    }
}
