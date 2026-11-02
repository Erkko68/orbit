package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.mapper.StoredExpense
import com.orbit.app.data.mapper.expenseDocument
import com.orbit.app.data.mapper.mapExpense
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.model.newestFirst
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ExpenseRepository
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FirebaseFirestore
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

    override suspend fun add(spaceId: String, expense: NewExpense): String {
        val document = expenses(spaceId).document
        val data = expenseDocument(spaceId, expense, auth.requireUser().id)
        writeLocally("Expense ${document.id}") { document.set(data) { encodeDefaults = false } }
        return document.id
    }

    override suspend fun edit(
        spaceId: String,
        expenseId: String,
        title: String,
        paidBy: String,
        split: Map<String, Long>,
        date: LocalDate,
    ) {
        val document = expenses(spaceId).document(expenseId)
        writeLocally("A change to expense $expenseId") {
            document.updateFields {
                FieldPath("title") to title
                FieldPath("paidBy") to paidBy
                FieldPath("split") to split
                FieldPath("date") to date.toString()
            }
        }
    }

    override suspend fun delete(spaceId: String, expenseId: String) {
        val document = expenses(spaceId).document(expenseId)
        writeLocally("Deleting expense $expenseId") { document.delete() }
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
        )
        return mapExpense(document.id, stored).also {
            if (it == null) Logger.w { "Skipping the malformed expense ${document.id}" }
        }
    }
}
