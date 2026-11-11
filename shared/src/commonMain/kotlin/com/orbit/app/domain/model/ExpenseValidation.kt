package com.orbit.app.domain.model

const val EXPENSE_TITLE_MAX_LENGTH = 120

sealed interface ExpenseError {
    data object BlankTitle : ExpenseError
    data object TitleTooLong : ExpenseError
    data object PayerNotMember : ExpenseError
    data object NoShares : ExpenseError
    data class NonPositiveShare(val memberId: String) : ExpenseError
    data class ShareNotMember(val memberId: String) : ExpenseError
    data object SharesDoNotAddUp : ExpenseError
}

/**
 * Every rule an expense must pass before it is written. Returns the errors instead of throwing so a form can
 * show them. [total] is the amount typed, which only the shares are stored from. Editing an expense passes the
 * members who left in [memberIds] too: their balance still counts (#56).
 */
fun validateExpense(
    title: String,
    paidBy: String,
    split: Map<String, Long>,
    total: Long,
    memberIds: Collection<String>,
): List<ExpenseError> = titleErrors(title) +
    listOfNotNull(
        ExpenseError.PayerNotMember.takeIf { paidBy !in memberIds },
        ExpenseError.NoShares.takeIf { split.isEmpty() },
        ExpenseError.SharesDoNotAddUp.takeIf { split.isNotEmpty() && split.values.sum() != total },
    ) +
    split.flatMap { (memberId, cents) ->
        listOfNotNull(
            ExpenseError.NonPositiveShare(memberId).takeIf { cents <= 0 },
            ExpenseError.ShareNotMember(memberId).takeIf { memberId !in memberIds },
        )
    }

private fun titleErrors(title: String): List<ExpenseError> {
    val trimmed = title.trim()
    return listOfNotNull(
        ExpenseError.BlankTitle.takeIf { trimmed.isEmpty() },
        ExpenseError.TitleTooLong.takeIf { trimmed.length > EXPENSE_TITLE_MAX_LENGTH },
    )
}
