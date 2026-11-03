package com.orbit.app.feature.assistant.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitQuestionCard
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.feature.assistant.InterviewUiState
import com.orbit.app.feature.assistant.InterviewViewModel
import com.orbit.app.feature.assistant.consent.RequireAiConsent
import com.orbit.app.feature.assistant.dictation.DictationButton
import com.orbit.app.feature.assistant.message
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.ic_close
import orbit.shared.generated.resources.interview_answer_placeholder
import orbit.shared.generated.resources.interview_error_title
import orbit.shared.generated.resources.interview_loading
import orbit.shared.generated.resources.interview_manual
import orbit.shared.generated.resources.interview_send
import orbit.shared.generated.resources.interview_title
import orbit.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The assistant's interview, full screen. It asks for consent first (#71); then one question at a time on its card,
 * with chips and an answer in the user's words, typed or dictated (#73). Whatever goes wrong leaves the manual path,
 * [onManual], which declining the consent takes too. When the assistant has a plan, [onPlanReady] opens the review.
 */
@Composable
fun InterviewScreen(
    onClose: () -> Unit,
    onManual: () -> Unit,
    onPlanReady: () -> Unit,
    viewModel: InterviewViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.planReady) { if (state.planReady) onPlanReady() }
    if (!state.consented) {
        RequireAiConsent(onResult = { accepted -> if (accepted) viewModel.onConsent() else onManual() })
    }
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = stringResource(Res.string.interview_title),
                navigationIcon = { OrbitIconButton(Res.drawable.ic_close, stringResource(Res.string.close), onClose) },
            )
        },
        scrollable = true,
    ) {
        InterviewContent(state, viewModel)
        OrbitTextButton(stringResource(Res.string.interview_manual), onManual, Modifier.fillMaxWidth())
    }
}

@Composable
private fun InterviewContent(state: InterviewUiState, viewModel: InterviewViewModel) {
    val error = state.error
    val question = state.question
    when {
        error != null && state.canRetry -> OrbitErrorState(
            title = stringResource(Res.string.interview_error_title),
            body = stringResource(error.message()),
            retryText = stringResource(Res.string.retry),
            onRetry = viewModel::retry,
            modifier = Modifier.fillMaxWidth(),
        )
        error != null -> OrbitEmptyState(
            title = stringResource(Res.string.interview_error_title),
            body = stringResource(error.message()),
            modifier = Modifier.fillMaxWidth(),
        )
        state.loading -> OrbitLoadingState(stringResource(Res.string.interview_loading), Modifier.fillMaxWidth())
        question != null -> {
            OrbitQuestionCard(question.text, question.options, state.selected, viewModel::select)
            if (question.freeText) {
                OrbitTextField(
                    value = state.text,
                    onValueChange = viewModel::onTextChange,
                    placeholder = stringResource(Res.string.interview_answer_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        DictationButton(onStart = viewModel::onDictationStart, onTranscript = viewModel::onDictation)
                    },
                )
            }
            OrbitButton(
                text = stringResource(Res.string.interview_send),
                onClick = viewModel::send,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSend,
            )
        }
    }
}
