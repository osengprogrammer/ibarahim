package com.example.crashcourse.ui

import com.example.crashcourse.db.*

/**
 * 🏛️ Azura Tech Options Helper
 * Standardizes access to Room Entities for the dynamic UI.
 */
object OptionsHelpers {

    fun getName(option: Any): String = when(option) {
        is ClassOption -> option.name
        is SubClassOption -> option.name
        is GradeOption -> option.name
        is SubGradeOption -> option.name
        is ProgramOption -> option.name
        is RoleOption -> option.name
        else -> ""
    }

    fun getOrder(option: Any): Int = when(option) {
        is ClassOption -> option.displayOrder
        is SubClassOption -> option.displayOrder
        is GradeOption -> option.displayOrder
        is SubGradeOption -> option.displayOrder
        is ProgramOption -> option.displayOrder
        is RoleOption -> option.displayOrder
        else -> 0
    }

    fun getParentId(option: Any): Int? = when (option) {
        is SubClassOption -> option.parentClassId
        is SubGradeOption -> option.parentGradeId
        else -> null
    }

    fun getId(option: Any): Int = when(option) {
        is ClassOption -> option.id
        is SubClassOption -> option.id
        is GradeOption -> option.id
        is SubGradeOption -> option.id
        is ProgramOption -> option.id
        is RoleOption -> option.id
        else -> 0 
    }

    /**
     * 🚀 IMMUTABLE UPDATE ENGINE
     * Returns a new instance of the data class with updated fields.
     */
    fun copyWith(
        option: Any, 
        newName: String? = null, 
        newOrder: Int? = null, 
        newParentId: Int? = null
    ): Any = when(option) {
        is ClassOption -> option.copy(
            name = newName ?: option.name,
            displayOrder = newOrder ?: option.displayOrder
        )
        is SubClassOption -> option.copy(
            name = newName ?: option.name,
            displayOrder = newOrder ?: option.displayOrder,
            // 🚀 FIX: Only update Parent if a new valid ID is provided
            parentClassId = newParentId ?: option.parentClassId
        )
        is GradeOption -> option.copy(
            name = newName ?: option.name,
            displayOrder = newOrder ?: option.displayOrder
        )
        is SubGradeOption -> option.copy(
            name = newName ?: option.name,
            displayOrder = newOrder ?: option.displayOrder,
            parentGradeId = newParentId ?: option.parentGradeId
        )
        is ProgramOption -> option.copy(
            name = newName ?: option.name,
            displayOrder = newOrder ?: option.displayOrder
        )
        is RoleOption -> option.copy(
            name = newName ?: option.name,
            displayOrder = newOrder ?: option.displayOrder
        )
        // 🛡️ Safety: If it's a type we don't recognize, return as is
        else -> option
    }
}