package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.mapper.StoredExpense
import com.orbit.app.data.mapper.activityDocument
import com.orbit.app.data.mapper.expenseDocument
import com.orbit.app.data.mapper.mapExpense
import com.orbit.app.domain.model.ActivityAction
import com.orbit.app.domain.model.ActivityTarget
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.model.newestFirst
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ExpenseRepository
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * The ledger in Firestore, read whole through a live listener that the offline cache answers first (ADR 0003).
 * The app sorts it, because the balances need every entry anyway and ordering by two fields would take an index.
 */
class FirestoreExpenseRepository(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
) : ExpenseRepository {
    /** Refused while a space made a moment ago is not on the server yet, so asked again for a few seconds. */
    override fun observeExpenses(spaceId: String): Flow<List<Expense>> =
        expenses(spaceId).snapshots.map { snapshot ->
            snapshot.documents.mapNotNull(::toExpense).newestFirst()
        }.retryWhileRefused()

    /** The expense and its entry in the history go in one batch (#83). */
    override suspend fun add(spaceId: String, expense: NewExpense): String {
        val userId = auth.requireUser().id
        val document = expenses(spaceId).document
        val batch = firestore.batch()
        batch.set(document, expenseDocument(spaceId, expense, userId)) { encodeDefaults = false }
        batch.logActivity(
            firestore, spaceId,
            activityDocument(userId, ActivityAction.CREATE, ActivityTarget.EXPENSE, document.id, expense.title),
        )
        writeLocally("Expense ${document.id}") { batch.commit() }
        return document.id
    }

    /**
     * Its entry in the history keeps the previous value of each field that differs, read from the cache the screen
     * keeps. The fields are compared as stored, so an edit that changes nothing still says it was saved.
     */
    override suspend fun edit(
        spaceId: String,
        expenseId: String,
        title: String,
        paidBy: String,
        split: Map<String, Long>,
        date: LocalDate,
        customAllocation: Map<String, Boolean>?,
    ) {
        val userId = auth.requireUser().id
        val document = expenses(spaceId).document(expenseId)
        val before = document.get(Source.CACHE)
        val changes = listOfNotNull(
            before.previous<String>("title").takeIf { before.field<String>("title") != title },
            before.previous<String>("paidBy").takeIf { before.field<String>("paidBy") != paidBy },
            before.previous<Map<String, Long>>("split").takeIf { before.field<Map<String, Long>>("split") != split },
            before.previous<String>("date").takeIf { before.field<String>("date") != date.toString() },
            before.previous<Map<String, Boolean>>(CUSTOM_ALLOCATION)
                .takeIf { before.field<Map<String, Boolean>>(CUSTOM_ALLOCATION) != customAllocation },
        ).toMap()
        val batch = firestore.batch()
        batch.updateFields(document) {
            FieldPath("title") to title
            FieldPath("paidBy") to paidBy
            FieldPath("split") to split
            FieldPath("date") to date.toString()
            FieldPath(CUSTOM_ALLOCATION) to (customAllocation ?: FieldValue.delete)
        }
        batch.logActivity(
            firestore, spaceId,
            activityDocument(userId, ActivityAction.EDIT, ActivityTarget.EXPENSE, expenseId, title, changes),
        )
        writeLocally("A change to expense $expenseId") { batch.commit() }
    }

    /** Its entry in the history keeps every field undo (#84) needs to put the expense back. */
    override suspend fun delete(spaceId: String, expenseId: String) {
        val userId = auth.requireUser().id
        val document = expenses(spaceId).document(expenseId)
        val before = document.get(Source.CACHE)
        val removed = listOf(
            before.previous<String>("title"),
            before.previous<String>("paidBy"),
            before.previous<Map<String, Long>>("split"),
            before.previous<String>("date"),
            before.previous<String>("createdBy"),
            before.previous<String>("kind"),
            before.previous<String>("itemId"),
            before.previous<Map<String, Boolean>>(CUSTOM_ALLOCATION),
        ).filterNot { it.second == "null" }.toMap()
        val batch = firestore.batch()
        batch.delete(document)
        batch.logActivity(
            firestore, spaceId,
            activityDocument(
                userId, ActivityAction.DELETE, ActivityTarget.EXPENSE, expenseId,
                before.field<String>("title").orEmpty(), removed,
            ),
        )
        writeLocally("Deleting expense $expenseId") { batch.commit() }
    }

    private fun expenses(spaceId: String) = firestore.collection(SPACES).document(spaceId).collection(EXPENSES)

    private fun toExpense(document: DocumentSnapshot): Expense? {
        val stored = StoredExpense(
            title = document.field("title"),
            paidBy = document.field("paidBy"),
            split = document.field("split"),
            date = document.field("date"),
            createdBy = document.field("createdBy"),
            createdAt = document.instant("createdAt"),
            kind = document.field("kind"),
            itemId = document.field("itemId"),
            customAllocation = document.field(CUSTOM_ALLOCATION),
        )
        return mapExpense(document.id, stored).also {
            if (it == null) Logger.w { "Skipping the malformed expense ${document.id}" }
        }
    }

    private companion object {
        const val CUSTOM_ALLOCATION = "customAllocation"
    }
}
