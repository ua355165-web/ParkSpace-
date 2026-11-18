package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.DemoAdminAuditLogEntity
import com.example.core.database.entity.DemoCommissionConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DemoAdminDao {
  @Query("SELECT * FROM demo_commission_config WHERE id = :id LIMIT 1")
  fun observeCommissionConfig(
    id: String = DemoCommissionConfigEntity.ACTIVE_CONFIG_ID,
  ): Flow<DemoCommissionConfigEntity?>

  @Query("SELECT * FROM demo_commission_config WHERE id = :id LIMIT 1")
  suspend fun getCommissionConfigOnce(
    id: String = DemoCommissionConfigEntity.ACTIVE_CONFIG_ID,
  ): DemoCommissionConfigEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCommissionConfig(config: DemoCommissionConfigEntity)

  @Query("SELECT * FROM demo_admin_audit_logs ORDER BY createdAtEpochMs DESC")
  fun observeAllAuditLogs(): Flow<List<DemoAdminAuditLogEntity>>

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertAuditLog(entry: DemoAdminAuditLogEntity)

  @Query("SELECT COUNT(*) FROM demo_admin_audit_logs")
  suspend fun countAuditLogs(): Int
}
