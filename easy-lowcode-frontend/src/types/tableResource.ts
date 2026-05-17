export interface DisplayFieldSetting {
  visible: boolean
  label?: string
  width?: number
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right'
  format?: string
  numberFormat?: string
  enumMapping?: Record<string, string>
  sortable?: boolean
}

export interface DisplaySettings {
  pageSize?: number
  stripe?: boolean
  border?: boolean
  fields: Record<string, DisplayFieldSetting>
}

export interface ProcessorConfig {
  type: string
  enabled: boolean
  order: number
  config: Record<string, any>
}

export interface TemplateParam {
  name: string
  type: string
  required: boolean
  defaultValue?: string
  label?: string
}

export interface QueryTemplate {
  name: string
  label: string
  description?: string
  sql: string
  enabled: boolean
  isDefault: boolean
  parameters: TemplateParam[]
}

export interface FieldConfig {
  columnName: string
  dataType: string
  columnComment?: string
  exactQuery: boolean
  fuzzyQuery: boolean
}

export interface ConfigJson {
  fields: FieldConfig[]
  parameterProcessors: ProcessorConfig[]
  resultProcessors: ProcessorConfig[]
  queryTemplates: QueryTemplate[]
  displaySettings: DisplaySettings
}

export const BUILTIN_PARAM_PROCESSORS = [
  { type: 'defaultValue', label: '默认值', description: '当参数缺失时填充默认值' },
  { type: 'paramMapping', label: '参数映射', description: '将外部参数名映射为内部字段名' },
  { type: 'paramValidator', label: '参数校验', description: '校验必填字段、参数类型' },
]

export const BUILTIN_RESULT_PROCESSORS = [
  { type: 'fieldFilter', label: '字段过滤', description: '白名单/黑名单模式过滤结果字段' },
  { type: 'dataMasking', label: '数据脱敏', description: '正则替换脱敏（手机/邮箱/身份证）' },
  { type: 'enumMapping', label: '枚举映射', description: '将编码值映射为可读标签' },
  { type: 'dateFormat', label: '日期格式化', description: '格式化日期/时间戳字段' },
]

export function createDefaultConfigJson(fields: FieldConfig[]): ConfigJson {
  const displayFields: Record<string, DisplayFieldSetting> = {}
  fields.forEach(f => {
    displayFields[f.columnName] = {
      visible: true,
      label: f.columnComment || f.columnName,
      width: 150,
      align: 'left',
      format: f.dataType?.toLowerCase().includes('time') || f.dataType?.toLowerCase().includes('date')
        ? 'yyyy-MM-dd HH:mm:ss' : undefined,
    }
  })

  return {
    fields,
    parameterProcessors: [],
    resultProcessors: [],
    queryTemplates: [
      {
        name: 'default',
        label: '默认查询',
        description: '按传入条件过滤查询',
        sql: 'SELECT * FROM {{tableName}} WHERE 1=1',
        enabled: true,
        isDefault: true,
        parameters: [],
      },
    ],
    displaySettings: {
      pageSize: 20,
      stripe: true,
      border: false,
      fields: displayFields,
    },
  }
}
