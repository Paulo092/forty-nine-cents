package com.fortyninecents.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fortyninecents.domain.Alias
import com.fortyninecents.domain.AliasMatch
import com.fortyninecents.domain.AliasResolver

@Entity(
    tableName = "aliases",
    // um apelido por identificador (+ tipo de correspondência); salvar de novo substitui
    indices = [Index(value = ["match_key", "match_type"], unique = true)],
)
data class AliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    /** [pattern] normalizado (sem acento, minúsculo) — garante a unicidade. */
    @ColumnInfo(name = "match_key") val matchKey: String,
    val nickname: String,
    @ColumnInfo(name = "match_type") val matchType: AliasMatch,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

fun AliasEntity.toDomain() = Alias(id = id, pattern = pattern, nickname = nickname, matchType = matchType)

fun Alias.toEntity() = AliasEntity(
    id = id,
    pattern = pattern.trim(),
    matchKey = AliasResolver.key(pattern),
    nickname = nickname.trim(),
    matchType = matchType,
)
