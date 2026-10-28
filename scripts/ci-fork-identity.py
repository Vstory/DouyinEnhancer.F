#!/usr/bin/env python3
# SCRIPT_VERSION=1.0.0
"""CI 专用：把 runner 工作区的包名、显示名与启动验证闸门改写成 fork 形态，仓库源码一行都不落库。

改四样东西：applicationId、显示名（含各 locale 与 Gradle 产物名前缀）、启动验证弹窗的闸门。

包名为什么落不进仓库：上游一旦改这些行，仓库里的 fork 值就是合并冲突；而 fork 身份只在出包
时才需要，故一律在 CI 工作区改写。

以「期望的原文 → 改写后」逐条声明，任一项找不到原文即整体失败：上游改了写法时必须回来同步，
不允许静默漏改（漏改的产物能装、能跑，只是装上去是上游那个应用）。

用法：
    python3 scripts/ci-fork-identity.py --app-id io.github.twyora.douyinenhancer.fork --label DouyinEnhancer.F
    python3 scripts/ci-fork-identity.py --dry-run
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

DEFAULT_APP_ID = "io.github.twyora.douyinenhancer.fork"
DEFAULT_LABEL = "DouyinEnhancer.F"
UPSTREAM_APP_ID = "io.github.twyora.douyinenhancer"


def build_edits(app_id: str, label: str) -> list[tuple[str, str, str]]:
    """(相对路径, 期望原文, 改写后)。原文出现次数不为 1 即失败。"""
    return [
        # 只改 applicationId（最终包名），不动 namespace：R 类、资源引用、类的全限定名都跟
        #   namespace 走，改它会让 manifest 里的 `.ui.MainActivity` 解析到不存在的类（能装、一开就崩）。
        ("gradle.properties", f"project.applicationId={UPSTREAM_APP_ID}", f"project.applicationId={app_id}"),
        # 显示名：launcher、LSPosed 模块列表、系统设置里的应用名都读这个
        (
            "app/src/main/res/values/strings.xml",
            '<string name="app_name">Douyin Enhancer</string>',
            f'<string name="app_name">{label}</string>',
        ),
        # 中文资源里另有一份 app_name（上游的中文名）—— 只改默认值的话，中文设备（fork 的主要
        #   使用场景）仍显示上游中文名，安装器界面看着就像装错了包
        (
            "app/src/main/res/values-zh-rCN/values-zh.xml",
            '<string name="app_name">抖柚</string>',
            f'<string name="app_name">{label}</string>',
        ),
        # Gradle 侧原始产物名（app/build/outputs/apk/.../DouyinEnhancer_<版本>.apk）：CI 发布名由
        #   workflow 单独拼，这里只管构建日志与本地产物目录里露出的名字 —— 留上游名会让人以为编的是上游包
        (
            "app/build.gradle.kts",
            'output.outputFileName.set("DouyinEnhancer_${vn}$buildTypeSuffix.apk")',
            f'output.outputFileName.set("{label}_${{vn}}$buildTypeSuffix.apk")',
        ),
        # 启动验证弹窗停用：shouldVerify 是唯一闸门（VerifyDialogHooker 的 onResume、SettingsDialog
        #   的入口都问它）⇒ 恒 false 即两个入口一起失效，不必删调用点（删了反而让上游合并更容易冲突）。
        (
            "app/src/main/java/io/github/twyora/douyinenhancer/ui/legacy/VerifyDialog.kt",
            """        fun shouldVerify(context: Context): Boolean {
            val isChineseLocale = runCatching {
                context.resources.configuration.locales[0].language ==
                    Locale.CHINESE.language
            }.onFailure {
                YLog.error("$TAG: failed to read device locale", it)
            }.getOrDefault(false)
            val lastVerifiedVersion = ConfigManager.module.lastVerifiedVersion.value

            return !(
                BuildConfig.DEBUG || !isChineseLocale ||
                    BuildConfig.VERSION_CODE == lastVerifiedVersion
                )
        }""",
            """        // fork：上游「输入模块下载地址」的启动验证弹窗在 fork 版停用
        fun shouldVerify(context: Context): Boolean = false""",
        ),
    ]


# 改写完成后逐条校验：新写法必须恰好 1 处、旧写法必须不再出现。
#   旧写法是新写法的前缀时（包名 …douyinenhancer → …douyinenhancer.fork）跳过子串比较，
#   否则必然误报残留。
def verify_edits(edits: list[tuple[str, str, str]]) -> list[str]:
    hits: list[str] = []
    for rel, old, new in edits:
        text = (REPO_ROOT / rel).read_text(encoding="utf-8")
        got = text.count(new)
        if got != 1:
            hits.append(f"{rel}: 改写后写法出现 {got} 处（应 1 处）")
        if old not in new and old in text:
            hits.append(f"{rel}: 仍残留旧写法: {old.splitlines()[0][:80]}")
    return hits


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--app-id", default=DEFAULT_APP_ID)
    parser.add_argument("--label", default=DEFAULT_LABEL)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    # label 不只进 app_name：它还当前缀进 APK 文件名与下载 URL ⇒ 非法字符在这里拦下
    if not re.fullmatch(r"[A-Za-z0-9._-]+", args.label):
        print(f"::error::--label 只允许 A-Za-z0-9._-（它进 APK 文件名与下载 URL）：{args.label!r}", file=sys.stderr)
        return 1
    if not re.fullmatch(r"[A-Za-z0-9._]+", args.app_id):
        print(f"::error::--app-id 不合法：{args.app_id!r}", file=sys.stderr)
        return 1

    edits = build_edits(args.app_id, args.label)
    problems: list[str] = []
    for rel, old, _new in edits:
        path = REPO_ROOT / rel
        if not path.is_file():
            problems.append(f"{rel}: 文件不存在")
            continue
        got = path.read_text(encoding="utf-8").count(old)
        if got != 1:
            problems.append(f"{rel}: 期望 1 处、实际 {got} 处: {old}")

    if problems:
        print("::error::fork 身份改写的前置校验未通过：", file=sys.stderr)
        for p in problems:
            print(f"  - {p}", file=sys.stderr)
        return 1

    if args.dry_run:
        for rel, old, new in edits:
            print(f"[dry-run] {rel}\n    - {old}\n    + {new}")
        return 0

    for rel, old, new in edits:
        path = REPO_ROOT / rel
        path.write_text(path.read_text(encoding="utf-8").replace(old, new), encoding="utf-8")
        print(f"  {rel}: {old} → {new}")

    hits = verify_edits(edits)
    if hits:
        print("::error::改写后身份值不正确：", file=sys.stderr)
        for h in hits:
            print(f"  - {h}", file=sys.stderr)
        return 1

    print(f"已改写为 fork 身份：appId={args.app_id} label={args.label}（共 {len(edits)} 项，仅工作区，不入库）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
