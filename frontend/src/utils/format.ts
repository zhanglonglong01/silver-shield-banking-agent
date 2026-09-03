export function money(value: string | number, currency = 'CNY') {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency }).format(Number(value))
}

export function dateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export function reasonLabel(code?: string | null) {
  const labels: Record<string, string> = {
    ALLOWED: '策略、权限与参数校验通过',
    EXPLICIT_CONFIRMATION_REQUIRED: '写操作已准备，等待独立用户确认',
    PLAN_COMPLETED: '计划中的全部步骤已完成',
    PLAN_CREATED: '已创建受约束任务计划',
    USER_RESUMED: '用户已恢复任务',
    USER_CANCELLED: '用户取消，未执行后续写操作',
    TOOL_NOT_IN_ALLOWED_SET: '工具不在本轮允许集合',
    RESOURCE_NOT_AVAILABLE: '资源不存在或不属于当前用户',
    AAL2_REQUIRED: '当前会话认证强度不足',
    PROVIDER_UNAVAILABLE: '模型服务不可用，系统安全降级',
    GROUNDING_FAILED: '回答缺少可验证执行证据'
  }
  return code ? (labels[code] ?? code) : '暂无原因码'
}
