package org.gradle.script.lang.kotlin.fixtures

import org.gradle.util.TextUtil


fun String.toPlatformLineSeparators() = TextUtil.toPlatformLineSeparators(this)
