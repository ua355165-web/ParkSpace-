package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.domain.model.AdminAuditAction
import com.example.core.domain.model.AdminAuditTargetType

@Entity(tableName = "demo_commission_config")
data class DemoCommissionConfigEntity(
  @PrimaryKey val id: String,
  val commissionPercent: Int,
  val minFeePkr: Int,
  val maxFeePkr: Int,
  val note: String,
  val updatedByAdminId: String? = null,
  val updatedAtEpochMs: Long,
) {
  companion object {
    const val ACTIVE_CONFIG_ID = "active_commission_rule"
  }
}

@Entity(tableName = "demo_admin_audit_logs")
data class DemoAdminAuditLogEntity(
  @PrimaryKey val id: String,
  val actorAdminId: String,
  val actorLabel: String,
  val actionType: AdminAuditAction,
  val targetType: AdminAuditTargetType,
  val targetId: String,
  val targetLabel: String,
  val outcomeSummary: String,
  val reasonOrNote: String? = null,
  val createdAtEpochMs: Long,
)
