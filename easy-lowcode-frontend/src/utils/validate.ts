/**
 * 表单验证规则工具
 */

// 常用验证规则
export const rules = {
  /** 必填项 */
  required: [
    { required: true, message: '此项为必填项', trigger: 'blur' }
  ],

  /** 必填项（实时验证） */
  requiredChange: [
    { required: true, message: '此项为必填项', trigger: 'change' }
  ],

  /** 用户名（字母开头，4-16位） */
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,15}$/, message: '用户名以字母开头，4-16位字母、数字或下划线', trigger: 'blur' }
  ],

  /** 密码（至少8位，包含大小写字母和数字） */
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8个字符', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[^]{8,}$/, message: '密码需包含大小写字母和数字', trigger: 'blur' }
  ],

  /** 简单密码（至少6位） */
  simplePassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],

  /** 邮箱 */
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],

  /** 手机号 */
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],

  /** 座机电话 */
  tel: [
    { pattern: /^0\d{2,3}-?\d{7,8}$/, message: '请输入正确的电话号码', trigger: 'blur' }
  ],

  /** URL */
  url: [
    { pattern: /^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)$/, message: '请输入正确的网址', trigger: 'blur' }
  ],

  /** 身份证号 */
  idCard: [
    { pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/, message: '请输入正确的身份证号', trigger: 'blur' }
  ],

  /** 整数 */
  integer: [
    { pattern: /^-?\d+$/, message: '请输入整数', trigger: 'blur' }
  ],

  /** 正整数 */
  positiveInteger: [
    { pattern: /^[1-9]\d*$/, message: '请输入正整数', trigger: 'blur' }
  ],

  /** 数字（可含小数） */
  number: [
    { pattern: /^-?\d+(\.\d+)?$/, message: '请输入数字', trigger: 'blur' }
  ],

  /** 金额（最多2位小数） */
  money: [
    { pattern: /^(([1-9]\d*)|0)(\.\d{1,2})?$/, message: '请输入正确的金额', trigger: 'blur' }
  ],

  /** IP地址 */
  ip: [
    { pattern: /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/, message: '请输入正确的IP地址', trigger: 'blur' }
  ],

  /** 端口号 */
  port: [
    { pattern: /^([0-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-4]\d{4}|65[0-4]\d{2}|655[0-2]\d|6553[0-5])$/, message: '请输入正确的端口号', trigger: 'blur' }
  ],

  /** 中文字符 */
  chinese: [
    { pattern: /^[\u4e00-\u9fa5]+$/, message: '只能输入中文', trigger: 'blur' }
  ],

  /** 英文和数字 */
  englishNumber: [
    { pattern: /^[a-zA-Z0-9]+$/, message: '只能输入英文和数字', trigger: 'blur' }
  ],

  /** 英文、数字和下划线 */
  usernameLike: [
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能输入英文、数字和下划线', trigger: 'blur' }
  ],

  /** 邮政编码 */
  postalCode: [
    { pattern: /^[1-9]\d{5}$/, message: '请输入正确的邮政编码', trigger: 'blur' }
  ],

  /** 车牌号 */
  carNumber: [
    { pattern: /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳]$/, message: '请输入正确的车牌号', trigger: 'blur' }
  ]
}

/**
 * 自定义验证器工厂
 */
export const validators = {
  /** 范围验证 */
  range: (min: number, max: number) => ({
    validator: (_: any, value: any, callback: any) => {
      if (value < min || value > max) {
        callback(new Error(`数值必须在 ${min} 到 ${max} 之间`))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }),

  /** 长度验证 */
  length: (min: number, max: number) => ({
    validator: (_: any, value: any, callback: any) => {
      if (!value) {
        callback()
        return
      }
      const len = String(value).length
      if (len < min || len > max) {
        callback(new Error(`长度必须在 ${min} 到 ${max} 个字符之间`))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }),

  /** 自定义正则验证 */
  pattern: (regex: RegExp, message: string) => ({
    pattern: regex,
    message,
    trigger: 'blur'
  }),

  /** 异步验证器示例 */
  async: (validatorFn: (value: any) => Promise<boolean | string>) => ({
    validator: async (_: any, value: any, callback: any) => {
      try {
        const result = await validatorFn(value)
        if (result === true) {
          callback()
        } else {
          callback(new Error(typeof result === 'string' ? result : '验证失败'))
        }
      } catch {
        callback(new Error('验证失败'))
      }
    },
    trigger: 'blur'
  })
}

/**
 * 组合验证规则
 */
export const composeRules = (...ruleArrays: any[][]) => {
  return ruleArrays.flat()
}
