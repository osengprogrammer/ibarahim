package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 🏛️ DAO untuk Kelas (Class)
 */
@Dao
interface ClassOptionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(option: ClassOption)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<ClassOption>)

    @Query("SELECT * FROM class_options ORDER BY displayOrder ASC")
    fun getAllOptions(): Flow<List<ClassOption>>

    @Query("SELECT * FROM class_options WHERE id = :id")
    suspend fun getOptionById(id: Int): ClassOption?

    @Update
    suspend fun update(option: ClassOption)

    @Delete
    suspend fun delete(option: ClassOption)
}

/**
 * 🏛️ DAO untuk Sub-Kelas (SubClass)
 */
@Dao
interface SubClassOptionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(option: SubClassOption)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<SubClassOption>)

    @Query("SELECT * FROM subclass_options ORDER BY displayOrder ASC")
    fun getAllOptions(): Flow<List<SubClassOption>>

    @Query("SELECT * FROM subclass_options WHERE parentClassId = :parentClassId ORDER BY displayOrder ASC")
    fun getOptionsForClass(parentClassId: Int): Flow<List<SubClassOption>>

    @Update
    suspend fun update(option: SubClassOption)

    @Delete
    suspend fun delete(option: SubClassOption)
}

/**
 * 🏛️ DAO untuk Jenjang (Grade)
 */
@Dao
interface GradeOptionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(option: GradeOption)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<GradeOption>)

    @Query("SELECT * FROM grade_options ORDER BY displayOrder ASC")
    fun getAllOptions(): Flow<List<GradeOption>>

    @Query("SELECT * FROM grade_options WHERE id = :id")
    suspend fun getOptionById(id: Int): GradeOption?

    @Update
    suspend fun update(option: GradeOption)

    @Delete
    suspend fun delete(option: GradeOption)
}

/**
 * 🏛️ DAO untuk Sub-Jenjang (SubGrade)
 */
@Dao
interface SubGradeOptionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(option: SubGradeOption)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<SubGradeOption>)

    @Query("SELECT * FROM subgrade_options ORDER BY displayOrder ASC")
    fun getAllOptions(): Flow<List<SubGradeOption>>

    @Query("SELECT * FROM subgrade_options WHERE parentGradeId = :parentGradeId ORDER BY displayOrder ASC")
    fun getOptionsForGrade(parentGradeId: Int): Flow<List<SubGradeOption>>

    @Query("SELECT * FROM subgrade_options WHERE id = :id")
    suspend fun getOptionById(id: Int): SubGradeOption?

    @Update
    suspend fun update(option: SubGradeOption)

    @Delete
    suspend fun delete(option: SubGradeOption)
}

/**
 * 🏛️ DAO untuk Program (Program)
 */
@Dao
interface ProgramOptionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(option: ProgramOption)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<ProgramOption>)

    @Query("SELECT * FROM program_options ORDER BY displayOrder ASC")
    fun getAllOptions(): Flow<List<ProgramOption>>

    @Query("SELECT * FROM program_options WHERE id = :id")
    suspend fun getOptionById(id: Int): ProgramOption?

    @Update
    suspend fun update(option: ProgramOption)

    @Delete
    suspend fun delete(option: ProgramOption)
}

/**
 * 🏛️ DAO untuk Peran (Role)
 */
@Dao
interface RoleOptionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(option: RoleOption)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<RoleOption>)

    @Query("SELECT * FROM role_options ORDER BY displayOrder ASC")
    fun getAllOptions(): Flow<List<RoleOption>>

    @Query("SELECT * FROM role_options WHERE id = :id")
    suspend fun getOptionById(id: Int): RoleOption?

    @Update
    suspend fun update(option: RoleOption)

    @Delete
    suspend fun delete(option: RoleOption)
}