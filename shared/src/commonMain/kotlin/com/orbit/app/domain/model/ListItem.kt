package com.orbit.app.domain.model

data class ListItem(
    val id: String,
    val spaceId: String,
    val listName: String,
    val text: String,
    val checked: Boolean,
)
