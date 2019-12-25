package xyz.kosmonaffft.dbrestr.service.api

import xyz.kosmonaffft.dbrestr.metadata.DatabaseMetadata

interface SqlService {

    fun selectMany(schema: String, table: String, offset: Long, limit: Long): String

    fun count(schema: String, table: String): String

    fun selectOne(schema: String, table: String, idColumns: List<String>): String

    fun insert(schema: String, table: String, databaseMetadata: DatabaseMetadata, actualColumns: Set<String>): String

    fun update(schema: String, table: String, idColumns: List<String>, databaseMetadata: DatabaseMetadata, actualColumns: Set<String>): String

    fun delete(schema: String, table: String, idColumns: List<String>): String
}