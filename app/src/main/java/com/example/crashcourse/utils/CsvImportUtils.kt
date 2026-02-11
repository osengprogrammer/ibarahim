package com.example.crashcourse.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 📊 Azura Tech CSV Utility
 * Standardizes CSV parsing for bulk student and staff registration.
 */
object CsvImportUtils {
    private const val TAG = "CsvImportUtils"
    
    data class CsvStudentData(
        val studentId: String,
        val name: String,
        val className: String = "",
        val subClass: String = "",
        val grade: String = "",
        val subGrade: String = "",
        val program: String = "",
        val role: String = "",
        val photoUrl: String = ""
    )
    
    data class CsvParseResult(
        val students: List<CsvStudentData>,
        val errors: List<String>,
        val totalRows: Int,
        val validRows: Int
    )
    
    suspend fun parseCsvFile(context: Context, uri: Uri): CsvParseResult {
        val students = mutableListOf<CsvStudentData>()
        val errors = mutableListOf<String>()
        var totalRows = 0
        var validRows = 0
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    var lineNumber = 0
                    var headers: List<String>? = null
                    
                    while (reader.readLine().also { line = it } != null) {
                        lineNumber++
                        totalRows++
                        
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.isEmpty()) {
                            totalRows-- // Skip empty lines
                            continue
                        }
                        
                        val columns = parseCsvLine(currentLine)
                        
                        // Handle Header Row
                        if (lineNumber == 1) {
                            headers = columns.map { it.trim().lowercase() }
                            totalRows-- 
                            continue
                        }

                        if (headers == null) {
                            errors.add("Line $lineNumber: Header CSV tidak ditemukan.")
                            continue
                        }

                        if (columns.isEmpty()) {
                            totalRows--
                            continue
                        }
                        
                        try {
                            val student = parseStudentRow(headers, columns, lineNumber)
                            if (student != null) {
                                students.add(student)
                                validRows++
                            } else {
                                errors.add("Baris $lineNumber: Data ID atau Nama kosong.")
                            }
                        } catch (e: Exception) {
                            errors.add("Baris $lineNumber: ${e.message ?: "Kesalahan format data"}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Gagal membaca file CSV: ${e.message ?: "Unknown error"}")
        }
        
        return CsvParseResult(
            students = students,
            errors = errors,
            totalRows = totalRows,
            validRows = validRows
        )
    }
    
    /**
     * Parse single CSV line handling quotes and escaped characters (RFC-4180).
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < line.length) {
            when (val char = line[i]) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ 
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString().trim())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
            i++
        }
        
        result.add(current.toString().trim())
        return result
    }
    
    /**
     * 🛡️ SURGICAL FIX: Standardizes roles and sanitizes class inputs.
     */
    private fun parseStudentRow(
        headers: List<String>,
        columns: List<String>,
        lineNumber: Int
    ): CsvStudentData? {
        val headerMap = headers.mapIndexed { index, header -> header to index }.toMap()
        
        fun getValue(headerNames: List<String>): String {
            for (header in headerNames) {
                headerMap[header]?.takeIf { it < columns.size }?.let { 
                    return columns[it].trim() 
                }
            }
            return ""
        }
        
        val studentId = getValue(listOf("studentid", "student_id", "id", "student id", "student"))
        val name = getValue(listOf("name", "fullname", "student_name", "full name"))
        
        if (studentId.isEmpty() || name.isEmpty()) {
            return null
        }

        // 🛡️ ROLE STANDARDIZATION
        // Maps various human inputs to system constants
        val rawRole = getValue(listOf("role", "type", "position")).uppercase()
        val finalRole = when {
            rawRole.contains("ADMIN") -> "ADMIN"
            rawRole.contains("TEACH") || rawRole.contains("GURU") -> "TEACHER"
            else -> "STUDENT" // Default
        }
        
        return CsvStudentData(
            studentId = studentId,
            name = name,
            // 🛡️ CLASS SANITIZATION: Trims and normalizes class names to prevent duplication in UI
            className = getValue(listOf("class", "classname", "class_name", "class name")).trim(),
            subClass = getValue(listOf("subclass", "sub_class", "sub class")).trim(),
            grade = getValue(listOf("grade", "level")).trim(),
            subGrade = getValue(listOf("subgrade", "sub_grade", "sub grade")).trim(),
            program = getValue(listOf("program", "course")).trim(),
            role = finalRole,
            photoUrl = getValue(listOf("photo", "photourl", "photo_url", "image", "photo url"))
        )
    }
    
    fun generateSampleCsv(): String {
        return """
            Student ID,Name,Class,Sub Class,Grade,Sub Grade,Program,Role,Photo URL
            STU001,John Doe,10-IPA-1,IPA,10,1,MIPA,Student,https://example.com/john.jpg
            STU002,Jane Smith,11-IPS-2,IPS,11,2,SOSHUM,Student,https://example.com/jane.jpg
            TEA001,Mr. Johnson,,,,,,Teacher,https://example.com/teacher.jpg
        """.trimIndent()
    }
}