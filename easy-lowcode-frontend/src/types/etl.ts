/**
 * ETL task matching EtlTask.java
 */
export interface EtlTask {
  id?: string
  taskName: string
  taskCode: string
  sourceDatasourceId: string
  sourceTable?: string
  sourceSql?: string
  readMode?: string
  targetDatasourceId: string
  targetTable: string
  writeMode?: string
  fieldMapping?: string
  transformRules?: string
  scheduleType?: string
  cronExpression?: string
  intervalSeconds?: number
  batchSize?: number
  skipError?: number
  status?: number
  remark?: string
  sourceDatasourceName?: string
  targetDatasourceName?: string
  lastExecStatus?: string
  lastExecTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * ETL task execution log matching EtlTaskLog.java
 */
export interface EtlTaskLog {
  id?: string
  taskId: string
  execStatus: string
  startTime: string
  endTime: string
  readCount: number
  writeCount: number
  skipCount: number
  errorMessage: string
  execDetail: string
  createTime?: string
  updateTime?: string
}
