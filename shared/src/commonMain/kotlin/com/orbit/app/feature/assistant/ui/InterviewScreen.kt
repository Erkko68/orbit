package com.orbit.app.feature.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.dateLabel
import com.orbit.app.core.designsystem.component.OrbitChatBubble
import com.orbit.app.core.designsystem.component.OrbitChatComposer
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitHeading
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.feature.assistant.ChatMessage
import com.orbit.app.feature.assistant.InterviewUiState
import com.orbit.app.feature.assistant.InterviewViewModel
import com.orbit.app.feature.assistant.consent.RequireAiConsent
import com.orbit.app.feature.assistant.dictation.DictationButton
import com.orbit.app.feature.assistant.message
import com.orbit.app.feature.assistant.title
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.ic_close
import orbit.shared.generated.resources.interview_answer_placeholder
import orbit.shared.generated.resources.interview_error_quota_until
import orbit.shared.generated.resources.interview_back
import orbit.shared.generated.resources.interview_decide
import orbit.shared.generated.resources.interview_decided
import orbit.shared.generated.resources.interview_loading
import orbit.shared.generated.resources.interview_progress
import orbit.shared.generated.resources.interview_send
import orbit.shared.generated.resources.interview_skip
import orbit.shared.generated.resources.interview_skipped
import orbit.shared.generated.resources.interview_title
import orbit.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The assistant's interview, as a chat. It asks for consent first (#71); then the assistant's questions and the user's
 * answers follow each other, newest at the bottom. The question waiting for an answer carries its chips, and the
 * composer under the chat takes the user's own words, typed or dictated (#73). When the assistant cannot answer, the
 * chat says why, with a retry when it can help. Closing, or declining the consent, goes back to where the manual way
 * is. When the assistant has a plan, [onPlanReady] opens the review. It is about [spaceId], or a new space when empty.
 */
@Composable
fun InterviewScreen(
    spaceId: String,
    onClose: () -> Unit,
    onPlanReady: () -> Unit,
    viewModel: InterviewViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.planReady) { if (state.planReady) onPlanReady() }
    if (!state.consented) {
        RequireAiConsent(onResult = { accepted -> if (accepted) viewModel.onConsent() else onClose() })
    }
    val question = state.question
    val error = state.error
    val thinking = stringResource(Res.string.interview_loading)
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = stringResource(Res.string.interview_title),
                navigationIcon = { OrbitIconButton(Res.drawable.ic_close, stringResource(Res.string.close), onClose) },
            )
        },
        bottomBar = {
            OrbitChatComposer(
                value = state.text,
                onValueChange = viewModel::onTextChange,
                placeholder = stringResource(Res.string.interview_answer_placeholder),
                sendDescription = stringResource(Res.string.interview_send),
                onSend = viewModel::send,
                enabled = state.canType,
                canSend = state.canSend,
                trailingIcon = if (state.canType) {
                    { DictationButton(onStart = viewModel::onDictationStart, onTranscript = viewModel::onDictation) }
                } else {
                    null
                },
            )
        },
        chat = true,
        list = {
            // Newest first: the chat grows up from the composer.
            if (state.loading) {
                item(key = "thinking") { OrbitChatBubble(mine = false, loading = true) { Text(thinking) } }
            }
            if (error != null) {
                item(key = "error") { ErrorBubble(error, state, viewModel) }
            }
            val messages = state.messages.asReversed()
            items(messages.size, key = { messages.size - it }) { index ->
                // The question waiting for an answer carries its place in the interview, its chips and its controls.
                MessageBubble(messages[index], if (index == 0) question else null, state, viewModel)
            }
        },
    )
}

/** Why the assistant cannot answer, with a retry when it can help and Back once something was answered. */
@Composable
private fun ErrorBubble(error: AssistantError, state: InterviewUiState, viewModel: InterviewViewModel) {
    OrbitChatBubble(mine = false) {
        OrbitHeading(
            title = stringResource(error.title()),
            subtitle = state.quotaResetsOn?.let { stringResource(Res.string.interview_error_quota_until, dateLabel(it)) }
                ?: stringResource(error.message()),
            titleColor = MaterialTheme.colorScheme.error,
        )
        if (state.canRetry) {
            OrbitTonalButton(stringResource(Res.string.retry), viewModel::retry, Modifier.fillMaxWidth())
        }
        if (state.canGoBack) {
            OrbitTextButton(stringResource(Res.string.interview_back), viewModel::back)
        }
    }
}

/** One message of the chat. The [waiting] question also shows its progress, its chips and its controls. */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    waiting: InterviewQuestion?,
    state: InterviewUiState,
    viewModel: InterviewViewModel,
) {
    OrbitChatBubble(mine = message.mine) {
        if (waiting != null) {
            Text(
                stringResource(Res.string.interview_progress, waiting.position, waiting.budget),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            when {
                message.skipped -> stringResource(Res.string.interview_skipped)
                message.decided -> stringResource(Res.string.interview_decided, message.text)
                else -> message.text
            },
        )
        if (waiting != null) {
            QuestionControls(waiting, state, viewModel)
        }
    }
}

/** The chips of the [waiting] question, then Back, Skip and Decide for me. */
@Composable
private fun QuestionControls(waiting: InterviewQuestion, state: InterviewUiState, viewModel: InterviewViewModel) {
    if (waiting.options.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        ) {
            for (option in waiting.options) {
                OrbitFilterChip(option, option in state.selected, { viewModel.select(option) })
            }
        }
    }
    FlowRow {
        if (state.canGoBack) {
            OrbitTextButton(stringResource(Res.string.interview_back), viewModel::back)
        }
        OrbitTextButton(stringResource(Res.string.interview_skip), viewModel::skip, enabled = state.canAct)
        if (state.canDecide) {
            OrbitTextButton(stringResource(Res.string.interview_decide), viewModel::decide)
        }
    }
}
