package com.maestro.lang.syntax

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

sealed interface MaestroSyntax {
    val key: String
    val textAttributesKey: TextAttributesKey get() = UNKNOWN_KEY

    sealed interface Command : MaestroSyntax {
        val prefix: String get() = "-"
        val allowedSubCommands: Set<Command> get() = emptySet()
        val acceptRawValue: Boolean get() = false
        val autoCompletionType: AutoCompletionType get() = AutoCompletionType.SAME_LINE
        val acceptUndefinedSubCommands: Boolean get() = false
        val acceptsFileReferences: Boolean get() = false
        val acceptsStringInterpolation: Boolean get() = false
        val documentationUrl: String get() = ""

        // Primary commands
        data object AppId : Command {
            override val prefix: String = ""
            override val key: String = "appId"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/appid"
        }

        data object Tags : Command {
            override val prefix: String = ""
            override val key: String = "tags"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val acceptUndefinedSubCommands: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/tags"
        }

        data object AddMedia : Command {
            override val key: String = "addMedia"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val acceptUndefinedSubCommands: Boolean = true
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/addmedia"
        }

        data object AssertVisible : Command {
            override val key: String = "assertVisible"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = SELECTORS + COMMON_ARGUMENTS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/assertvisible"
        }

        data object AssertNotVisible : Command {
            override val key: String = "assertNotVisible"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = SELECTORS + COMMON_ARGUMENTS
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/assertnotvisible"
        }

        data object AssertTrue : Command {
            override val key: String = "assertTrue"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = SELECTORS + COMMON_ARGUMENTS + setOf(Condition)
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/asserttrue"
        }

        data object AssertWithAI : Command {
            override val key: String = "assertWithAI"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(Assertion)
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/assertwithai"
        }

        data object AssertNoDefectsWithAi : Command {
            override val key: String = "assertNoDefectsWithAi"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val documentationUrl: String =
                "https://docs.maestro.dev/api-reference/commands/assertnodefectswithai"
        }

        data object Back : Command {
            override val key: String = "back"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = emptySet()
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/back"
        }

        data object ClearKeychain : Command {
            override val key: String = "clearKeychain"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/clearkeychain"
        }

        data object ClearState : Command {
            override val key: String = "clearState"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/clearstate"
        }

        data object CopyTextFrom : Command {
            override val key: String = "copyTextFrom"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + SELECTORS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/copytextfrom"
        }

        data object EvalScript : Command {
            override val key: String = "evalScript"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/evalscript"
        }

        data object EraseText : Command {
            override val key: String = "eraseText"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/erasetext"
        }

        data object ExtendedWaitUntil : Command {
            override val key: String = "extendedWaitUntil"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = SELECTORS + COMMON_ARGUMENTS + setOf(
                Visible, NotVisible, Timeout
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/extendedwaituntil"
        }

        data object ExtractTextWithAI : Command {
            override val key: String = "extractTextWithAI"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(Query, OutputVariable)
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/extracttextwithai"
        }

        data object HideKeyboard : Command {
            override val key: String = "hideKeyboard"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/hidekeyboard"
        }

        data object InputText : Command {
            override val key: String = "inputText"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + SELECTORS
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/inputtext"
        }

        data object InputRandomEmail : Command {
            override val key: String = "inputRandomEmail"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + Length
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/inputrandomemail"
        }

        data object InputRandomPersonName : Command {
            override val key: String = "inputRandomPersonName"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + Length
            override val documentationUrl: String =
                "https://docs.maestro.dev/api-reference/commands/inputrandompersonname"
        }

        data object InputRandomNumber : Command {
            override val key: String = "inputRandomNumber"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + Length
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/inputrandomnumber"
        }

        data object InputRandomText : Command {
            override val key: String = "inputRandomText"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + Length
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/inputrandomtext"
        }

        data object KillApp : Command {
            override val key: String = "killApp"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/killapp"
        }

        data object LaunchApp : Command {
            override val key: String = "launchApp"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                AppId,
                ClearState,
                ClearKeychain,
                StopApp,
                Permissions,
                Arguments
            )
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/launchapp"
        }

        data object OpenLink : Command {
            override val key: String = "openLink"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                Link, AutoVerify, Browser
            )
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/openlink"
        }

        data object PressKey : Command {
            override val key: String = "pressKey"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/presskey"
        }

        data object PasteText : Command {
            override val key: String = "pasteText"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/pastetext"
        }

        data object AssertMarketingEvent : Command {
            override val key: String = "assertMarketingEvent"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> =
                COMMON_ARGUMENTS + setOf(Query, EventType, Parameters, Rules)
        }

        data object AssertVisual : Command {
            override val key: String = "assertVisual"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> =
                COMMON_ARGUMENTS + setOf(
                    Baseline, ThresholdPercentage, DelayMs, SilentError, CropOnRootContent, CropOn
                )
        }

        data object AssertMarketingEvents : Command {
            override val key: String = "assertMarketingEvents"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(Query, EventType, MinSize, MaxSize)
        }

        data object ClearMarketingEvents : Command {
            override val key: String = "clearMarketingEvents"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Repeat : Command {
            override val key: String = "repeat"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                Times, While, Commands
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/repeat"
        }

        data object Retry : Command {
            override val key: String = "retry"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                MaxRetries, Commands, File
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/retry"
        }

        data object RunFlow : Command {
            override val key: String = "runFlow"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val acceptsFileReferences: Boolean = true
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                File, Env, Commands, When
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/runflow"
        }

        data object RunScript : Command {
            override val key: String = "runScript"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val acceptsFileReferences: Boolean = true
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(File, Env)
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/runscript"
        }

        data object Scroll : Command {
            override val key: String = "scroll"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/scroll"
        }

        data object ScrollUntilVisible : Command {
            override val key: String = "scrollUntilVisible"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                Element, Direction, Timeout, Speed, VisibilityPercentage, CenterElement
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/scrolluntilvisible"
        }

        data object SetAirplaneMode : Command {
            override val key: String = "setAirplaneMode"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/setairplanemode"
        }

        data object SetLocation : Command {
            override val key: String = "setLocation"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                Latitude, Longitude
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/setlocation"
        }

        data object StartRecording : Command {
            override val key: String = "startRecording"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                Path
            )
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/startrecording"
        }

        data object StopApp : Command {
            override val key: String = "stopApp"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/stopapp"
        }

        data object StopRecording : Command {
            override val key: String = "stopRecording"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/stoprecording"
        }

        data object Swipe : Command {
            override val key: String = "swipe"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                Start, End, Duration, Direction, From
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/swipe"
        }

        data object TakeScreenshot : Command {
            override val key: String = "takeScreenshot"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                Path

            )
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/takescreenshot"
        }

        data object ToggleAirplaneMode : Command {
            override val key: String = "toggleAirplaneMode"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/toggleairplanemode"
        }

        data object TapOn : Command {
            override val key = "tapOn"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val acceptRawValue: Boolean = true
            override val allowedSubCommands: Set<Command> = SELECTORS + COMMON_ARGUMENTS + setOf(
                Repeat, Delay, RetryTapIfNoChange,
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/tapon"
        }

        data object DoubleTapOn : Command {
            override val key: String = "doubleTapOn"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val acceptRawValue: Boolean = true
            override val allowedSubCommands: Set<Command> = SELECTORS + COMMON_ARGUMENTS + setOf(
                Repeat, Delay, RetryTapIfNoChange,
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/doubletapon"
        }

        data object LongPressOn : Command {
            override val key: String = "longPressOn"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = SELECTORS + COMMON_ARGUMENTS + setOf(
                Repeat, Delay, RetryTapIfNoChange,
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/longpresson"
        }

        data object Travel : Command {
            override val key: String = "travel"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + setOf(
                Points, Speed
            )
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/travel"
        }

        data object WaitForAnimationToEnd : Command {
            override val key: String = "waitForAnimationToEnd"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + Timeout
            override val documentationUrl: String =
                "https://docs.maestro.dev/api-reference/commands/waitforanimationtoend"
        }

        data object OnFlowStart : Command {
            override val prefix: String = ""
            override val key: String = "onFlowStart"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val acceptUndefinedSubCommands: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/onflowstart"
        }

        data object OnFlowComplete : Command {
            override val prefix: String = ""
            override val key: String = "onFlowComplete"
            override val textAttributesKey: TextAttributesKey = ROOT_KEY
            override val acceptUndefinedSubCommands: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/commands/onflowcomplete"
        }

        // Secondary commands
        data object File : Command {
            override val prefix: String = ""
            override val key: String = "file"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptsFileReferences: Boolean = true
            override val acceptRawValue: Boolean = true
        }

        data object Baseline : Command {
            override val prefix: String = ""
            override val key: String = "baseline"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object ThresholdPercentage : Command {
            override val prefix: String = ""
            override val key: String = "thresholdPercentage"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object DelayMs : Command {
            override val prefix: String = ""
            override val key: String = "delayMs"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object SilentError : Command {
            override val prefix: String = ""
            override val key: String = "silentError"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object CropOnRootContent : Command {
            override val prefix: String = ""
            override val key: String = "cropOnRootContent"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object CropOn : Command {
            override val prefix: String = ""
            override val key: String = "cropOn"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + SELECTORS
        }

        data object Timeout : Command {
            override val prefix: String = ""
            override val key: String = "timeout"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Delay : Command {
            override val prefix: String = ""
            override val key: String = "delay"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object EventType : Command {
            override val key: String = "eventType"
            override val prefix: String = ""
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Parameters : Command {
            override val key: String = "parameters"
            override val prefix: String = ""
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptUndefinedSubCommands: Boolean = true
        }

        data object Rules : Command {
            override val key: String = "rules"
            override val prefix: String = ""
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptUndefinedSubCommands: Boolean = true
        }

        data object RetryTapIfNoChange : Command {
            override val prefix: String = ""
            override val key: String = "retryTapIfNoChange"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Arguments : Command {
            override val key: String = "arguments"
            override val prefix: String = ""
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptUndefinedSubCommands: Boolean = true
        }

        data object Speed : Command {
            override val prefix: String = ""
            override val key: String = "speed"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Points : Command {
            override val prefix: String = ""
            override val key: String = "points"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptUndefinedSubCommands: Boolean = true
        }

        data object VisibilityPercentage : Command {
            override val prefix: String = ""
            override val key: String = "visibilityPercentage"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object CenterElement : Command {
            override val prefix: String = ""
            override val key: String = "centerElement"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Env : Command {
            override val prefix: String = ""
            override val key: String = "env"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptUndefinedSubCommands: Boolean = true
        }

        data object User : Command {
            override val prefix: String = ""
            override val key: String = "USER"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
        }

        data object MaxRetries : Command {
            override val prefix: String = ""
            override val key: String = "maxRetries"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Permissions : Command {
            override val prefix: String = ""
            override val key: String = "permissions"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
            override val acceptUndefinedSubCommands: Boolean = true
        }

        data object Latitude : Command {
            override val prefix: String = ""
            override val key: String = "latitude"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Longitude : Command {
            override val prefix: String = ""
            override val key: String = "longitude"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Link : Command {
            override val prefix: String = ""
            override val key: String = "link"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object AutoVerify : Command {
            override val prefix: String = ""
            override val key: String = "autoVerify"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Browser : Command {
            override val prefix: String = ""
            override val key: String = "browser"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Element : Command {
            override val prefix: String = ""
            override val key: String = "element"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + SELECTORS
        }

        data object Type : Command {
            override val prefix: String = ""
            override val key: String = "TYPE"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
        }

        data object When : Command {
            override val prefix: String = ""
            override val key: String = "when"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
        }

        data object Visible : Command {
            override val prefix: String = ""
            override val key: String = "visible"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + SELECTORS
            override val acceptRawValue: Boolean = true
        }

        data object NotVisible : Command {
            override val prefix: String = ""
            override val key: String = "notVisible"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + SELECTORS
            override val acceptRawValue: Boolean = true
        }

        data object MinSize : Command {
            override val key: String = "minSize"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object MaxSize : Command {
            override val key: String = "maxSize"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Commands : Command {
            override val prefix: String = ""
            override val key: String = "commands"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
        }

        data object Id : Command {
            override val key = "id"
            override val allowedSubCommands: Set<Command> = emptySet()
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Times : Command {
            override val key = "times"
            override val allowedSubCommands: Set<Command> = emptySet()
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Text : Command {
            override val key = "text"
            override val allowedSubCommands: Set<Command> = emptySet()
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptsStringInterpolation: Boolean = true
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Direction : Command {
            override val prefix: String = ""
            override val key: String = "direction"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Start : Command {
            override val prefix: String = ""
            override val key: String = "start"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object End : Command {
            override val prefix: String = ""
            override val key: String = "end"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Duration : Command {
            override val prefix: String = ""
            override val key: String = "duration"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Label : Command {
            override val prefix: String = ""
            override val key: String = "label"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Optional : Command {
            override val prefix: String = ""
            override val key: String = "optional"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Index : Command {
            override val prefix: String = ""
            override val key: String = "index"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object True : Command {
            override val prefix: String = ""
            override val key: String = "true"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
        }

        data object While : Command {
            override val prefix: String = ""
            override val key: String = "while"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
            override val allowedSubCommands: Set<Command> = ACTION_COMMANDS.toSet()
        }

        data object WaitToSettleTimeoutMs : Command {
            override val prefix: String = ""
            override val key: String = "waitToSettleTimeoutMs"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object From : Command {
            override val prefix: String = ""
            override val key: String = "from"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
            override val allowedSubCommands: Set<Command> = SELECTORS
        }

        data object Point : Command {
            override val prefix: String = ""
            override val key: String = "point"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Width : Command {
            override val prefix: String = ""
            override val key: String = "width"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Height : Command {
            override val prefix: String = ""
            override val key: String = "height"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Tolerance : Command {
            override val prefix: String = ""
            override val key: String = "tolerance"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Enabled : Command {
            override val prefix: String = ""
            override val key: String = "enabled"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Checked : Command {
            override val prefix: String = ""
            override val key: String = "checked"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Focused : Command {
            override val prefix: String = ""
            override val key: String = "focused"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Selected : Command {
            override val prefix: String = ""
            override val key: String = "selected"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object RightOf : Command {
            override val prefix: String = ""
            override val key: String = "rightOf"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object LeftOf : Command {
            override val prefix: String = ""
            override val key: String = "leftOf"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Above : Command {
            override val prefix: String = ""
            override val key: String = "above"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Below : Command {
            override val prefix: String = ""
            override val key: String = "below"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object ChildOf : Command {
            override val prefix: String = ""
            override val key: String = "childOf"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }

        data object Condition : Command {
            override val prefix: String = ""
            override val key: String = "condition"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Path : Command {
            override val prefix: String = ""
            override val key: String = "path"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Assertion : Command {
            override val prefix: String = ""
            override val key: String = "assertion"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Query : Command {
            override val prefix: String = ""
            override val key: String = "query"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object OutputVariable : Command {
            override val prefix: String = ""
            override val key: String = "outputVariable"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object Length : Command {
            override val prefix: String = ""
            override val key: String = "length"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val acceptRawValue: Boolean = true
        }

        data object ContainsDescendants : Command {
            override val prefix: String = ""
            override val key: String = "containsDescendants"
            override val textAttributesKey: TextAttributesKey = ARGUMENT_KEY
            override val allowedSubCommands: Set<Command> = COMMON_ARGUMENTS + SELECTORS
            override val documentationUrl: String = "https://docs.maestro.dev/api-reference/selectors"
        }
    }

    data object Dash : MaestroSyntax {
        override val key = "-"
    }

    data object TripleDash : MaestroSyntax {
        override val key = "---"
    }

    data object InvalidSyntax : MaestroSyntax {
        override val key: String = ""
    }

    companion object {
        fun createFromValue(value: String): MaestroSyntax {
            return ALL_COMMANDS[value] ?: InvalidSyntax
        }

        val ROOT_KEY = TextAttributesKey.createTextAttributesKey(
            "MAESTRO_ROOT_KEY",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        val ARGUMENT_KEY = TextAttributesKey.createTextAttributesKey(
            "MAESTRO_ARGUMENT_KEY",
            DefaultLanguageHighlighterColors.INSTANCE_METHOD
        )

        val UNKNOWN_KEY = TextAttributesKey.createTextAttributesKey(
            "MAESTRO_UNKNOWN_KEY",
            DefaultLanguageHighlighterColors.IDENTIFIER
        )

        val FILE_REFERENCE_KEY = TextAttributesKey.createTextAttributesKey(
            "MAESTRO_FILE_REFERENCE_KEY",
            DefaultLanguageHighlighterColors.STRING
        )

        val STRING_INTERPOLATION_KEY = TextAttributesKey.createTextAttributesKey(
            "MAESTRO_STRING_INTERPOLATION_KEY",
            DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR
        )

        val OUTPUT_REFERENCE_KEY = TextAttributesKey.createTextAttributesKey(
            "MAESTRO_OUTPUT_REFERENCE_KEY",
            DefaultLanguageHighlighterColors.METADATA
        )

        val ALL_COMMANDS by lazy {
            buildMap<String, MaestroSyntax> {
                Command::class.sealedSubclasses.forEach { subclass ->
                    val instance = subclass.objectInstance ?: return@forEach
                    put(instance.key, instance)
                }
            }
        }

        val ROOT_COMMANDS by lazy {
            ALL_COMMANDS.values
                .filterIsInstance<Command>()
                .filter { it.prefix == "-" }
        }

        val ACTION_COMMANDS by lazy {
            ALL_COMMANDS.values
                .filterIsInstance<Command>()
                .filter { it.prefix.isEmpty() }
        }

        val SELECTORS by lazy {
            setOf(
                Command.Text,
                Command.Id,
                Command.Index,
                Command.Point,
                Command.Width,
                Command.Height,
                Command.Tolerance,
                Command.Enabled,
                Command.Checked,
                Command.Focused,
                Command.Selected,
                Command.RightOf,
                Command.LeftOf,
                Command.Above,
                Command.Below,
                Command.ChildOf,
                Command.WaitToSettleTimeoutMs,
                Command.ContainsDescendants
            )
        }

        val COMMON_ARGUMENTS by lazy {
            setOf(
                Command.Label,
                Command.Optional
            )
        }
    }

    enum class AutoCompletionType {
        SAME_LINE, NEXT_LINE
    }
}