package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.crashcourse.utils.Constants // 🚀 Bridge Anchor

// ==========================================
// 1. KELAS (Class) DAO
// ==========================================
@Dao
interface ClassOptionDao {
    @Query("SELECT * FROM ${Constants.COLL_OPT_CLASSES} ORDER BY displayOrder ASC")
    fun getAll(): Flow<List<ClassOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<ClassOption>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: ClassOption)

    @Delete
    suspend fun delete(option: ClassOption)
    
    @Query("DELETE FROM ${Constants.COLL_OPT_CLASSES}")
    suspend fun deleteAll()
}

// ==========================================
// 2. SUB-KELAS (SubClass) DAO
// ==========================================
@Dao
interface SubClassOptionDao {
    @Query("SELECT * FROM ${Constants.COLL_OPT_SUBCLASSES} ORDER BY displayOrder ASC")
    fun getAll(): Flow<List<SubClassOption>>

    @Query("SELECT * FROM ${Constants.COLL_OPT_SUBCLASSES} WHERE parentClassId = :parentClassId ORDER BY displayOrder ASC")
    fun getByParent(parentClassId: Int): Flow<List<SubClassOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<SubClassOption>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: SubClassOption)

    @Delete
    suspend fun delete(option: SubClassOption)

    @Query("DELETE FROM ${Constants.COLL_OPT_SUBCLASSES}")
    suspend fun deleteAll()
}

// ==========================================
// 3. JENJANG (Grade) DAO
// ==========================================
@Dao
interface GradeOptionDao {
    @Query("SELECT * FROM ${Constants.COLL_OPT_GRADES} ORDER BY displayOrder ASC")
    fun getAll(): Flow<List<GradeOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<GradeOption>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: GradeOption)

    @Delete
    suspend fun delete(option: GradeOption)

    @Query("DELETE FROM ${Constants.COLL_OPT_GRADES}")
    suspend fun deleteAll()
}

// ==========================================
// 4. SUB-JENJANG (SubGrade) DAO
// ==========================================
@Dao
interface SubGradeOptionDao {
    @Query("SELECT * FROM ${Constants.COLL_OPT_SUBGRADES} ORDER BY displayOrder ASC")
    fun getAll(): Flow<List<SubGradeOption>>

    @Query("SELECT * FROM ${Constants.COLL_OPT_SUBGRADES} WHERE parentGradeId = :parentGradeId ORDER BY displayOrder ASC")
    fun getByParent(parentGradeId: Int): Flow<List<SubGradeOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<SubGradeOption>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: SubGradeOption)

    @Delete
    suspend fun delete(option: SubGradeOption)

    @Query("DELETE FROM ${Constants.COLL_OPT_SUBGRADES}")
    suspend fun deleteAll()
}

// ==========================================
// 5. PROGRAM (Program) DAO
// ==========================================
@Dao
interface ProgramOptionDao {
    @Query("SELECT * FROM ${Constants.COLL_OPT_PROGRAMS} ORDER BY displayOrder ASC")
    fun getAll(): Flow<List<ProgramOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<ProgramOption>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: ProgramOption)

    @Delete
    suspend fun delete(option: ProgramOption)

    @Query("DELETE FROM ${Constants.COLL_OPT_PROGRAMS}")
    suspend fun deleteAll()
}

// ==========================================
// 6. PERAN (Role) DAO
// ==========================================
@Dao
interface RoleOptionDao {
    @Query("SELECT * FROM ${Constants.COLL_OPT_ROLES} ORDER BY displayOrder ASC")
    fun getAll(): Flow<List<RoleOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<RoleOption>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: RoleOption)

    @Delete
    suspend fun delete(option: RoleOption)

    @Query("DELETE FROM ${Constants.COLL_OPT_ROLES}")
    suspend fun deleteAll()
}