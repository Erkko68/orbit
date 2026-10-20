package com.orbit.app.feature.item

import com.orbit.app.domain.model.Role
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.role_admin
import orbit.shared.generated.resources.role_member
import orbit.shared.generated.resources.role_owner
import org.jetbrains.compose.resources.StringResource

/** What the UI calls each membership role in a space. */
val Role.label: StringResource
    get() = when (this) {
        Role.OWNER -> Res.string.role_owner
        Role.ADMIN -> Res.string.role_admin
        Role.MEMBER -> Res.string.role_member
    }
