package com.example.crashcourse.ui

import com.example.crashcourse.db.ClassOption
import com.example.crashcourse.db.SubClassOption
import com.example.crashcourse.db.GradeOption
import com.example.crashcourse.db.SubGradeOption
import com.example.crashcourse.db.ProgramOption
import com.example.crashcourse.db.RoleOption

/**
 * Helper object untuk standarisasi akses data Master (Polymorphic Mapping).
 * Memastikan UI tidak perlu tahu tipe data spesifik saat melakukan operasi dasar.
 */
object OptionsHelpers {

    // 🚀 Ambil Nama dari tabel manapun
    fun getName(option: Any): String = when(option) {
        is ClassOption -> option.name
        is SubClassOption -> option.name
        is GradeOption -> option.name
        is SubGradeOption -> option.name
        is ProgramOption -> option.name
        is RoleOption -> option.name
        else -> ""
    }

    // 🚀 Ambil Urutan Tampilan
    fun getOrder(option: Any): Int = when(option) {
        is ClassOption -> option.displayOrder
        is SubClassOption -> option.displayOrder
        is GradeOption -> option.displayOrder
        is SubGradeOption -> option.displayOrder
        is ProgramOption -> option.displayOrder
        is RoleOption -> option.displayOrder
        else -> 0
    }

    // 🚀 Ambil ID Relasi (Untuk Sub-Kategori)
    fun getParentId(option: Any): Int? = when (option) {
        is SubClassOption -> option.parentClassId
        is SubGradeOption -> option.parentGradeId
        else -> null
    }

    // 🚀 Update Nama (Immutable Copy)
    fun setName(option: Any, new: String): Any = when(option) {
        is ClassOption -> option.copy(name = new)
        is SubClassOption -> option.copy(name = new)
        is GradeOption -> option.copy(name = new)
        is SubGradeOption -> option.copy(name = new)
        is ProgramOption -> option.copy(name = new)
        is RoleOption -> option.copy(name = new)
        else -> option
    }

    // 🚀 Update Urutan
    fun setOrder(option: Any, new: Int): Any = when(option) {
        is ClassOption -> option.copy(displayOrder = new)
        is SubClassOption -> option.copy(displayOrder = new)
        is GradeOption -> option.copy(displayOrder = new)
        is SubGradeOption -> option.copy(displayOrder = new)
        is ProgramOption -> option.copy(displayOrder = new)
        is RoleOption -> option.copy(displayOrder = new)
        else -> option
    }

    // 🚀 Update Relasi Parent secara dinamis
    fun setParentId(option: Any, parentId: Int?): Any = when (option) {
        is SubClassOption -> option.copy(parentClassId = parentId ?: option.parentClassId)
        is SubGradeOption -> option.copy(parentGradeId = parentId ?: option.parentGradeId)
        else -> option
    }

    // 🚀 Ambil Primary Key (ID)
    fun getId(option: Any): Int = when(option) {
        is ClassOption -> option.id
        is SubClassOption -> option.id
        is GradeOption -> option.id
        is SubGradeOption -> option.id
        is ProgramOption -> option.id
        is RoleOption -> option.id
        else -> -1
    }
}