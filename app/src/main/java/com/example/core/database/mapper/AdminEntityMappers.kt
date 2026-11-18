package com.example.core.database.mapper

import com.example.core.database.entity.DemoAdminAuditLogEntity
import com.example.core.database.entity.DemoCommissionConfigEntity
import com.example.core.domain.model.AdminAuditLogEntry
import com.example.core.domain.model.DemoCommissionRule

fun DemoCommissionConfigEntity.toDomain(): DemoCommissionRule =
  DemoCommissionRule(
    commissionPercent = commissionPercent,
    minFeePkr = minFeePkr,
    maxFeePkr = maxFeePkr,
    note = note,
    updatedByAdminId = updatedByAdminId,
    updatedAtEpochMs = updatedAtEpochMs,
    isDefaultBaseline = false,
  )

fun DemoCommissionRule.toEntity(): DemoCommissionConfigEntity =
  DemoCommissionConfigEntity(
    id = DemoCommissionConfigEntity.ACTIVE_CONFIG_ID,
    commissionPercent = commissionPercent,
    minFeePkr = minFeePkr,
    maxFeePkr = maxFeePkr,
    note = note,
    updatedByAdminId = updatedByAdminId,
    updatedAtEpochMs = updatedAtEpochMs ?: System.currentTimeMillis(),
  )

fun DemoAdminAuditLogEntity.toDomain(): AdminAuditLogEntry =
  AdminAuditLogEntry(
    id = id,
    actorAdminId = actorAdminId,
    actorLabel = actorLabel,
    actionType = actionType,
    targetType = targetType,
    targetId = targetId,
    targetLabel = targetLabel,
    outcomeSummary = outcomeSummary,
    reasonOrNote = reasonOrNote,
    createdAtEpochMs = createdAtEpochMs,
  )

fun AdminAuditLogEntry.toEntity(): DemoAdminAuditLogEntity =
  DemoAdminAuditLogEntity(
    id = id,
    actorAdminId = actorAdminId,
    actorLabel = actorLabel,
    actionType = actionType,
    targetType = targetType,
    targetId = targetId,
    targetLabel = targetLabel,
    outcomeSummary = outcomeSummary,
    reasonOrNote = reasonOrNote,
    createdAtEpochMs = createdAtEpochMs,
  )
