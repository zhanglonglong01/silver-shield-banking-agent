<script setup lang="ts">
import { ref } from 'vue'
import { CheckCircle2, DatabaseBackup, LockKeyhole, ShieldCheck, Workflow } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import { useAuthStore } from '@/stores/auth'
import { apiRequest } from '@/services/http'
const auth = useAuthStore(); const resetResult = ref<Record<string, unknown> | null>(null); const resetError = ref(''); const resetting = ref(false)
async function reset() { resetting.value = true; resetError.value = ''; try { resetResult.value = await apiRequest<Record<string, unknown>>('/api/v1/sandbox/reset', { method: 'POST' }) } catch (e) { resetError.value = e instanceof Error ? e.message : '重置失败' } finally { resetting.value = false } }
</script>

<template><div class="page"><PageHeader eyebrow="ZERO TRUST EXECUTION" title="安全与沙箱" description="模型建议和真实执行之间，始终隔着确定性的权限、风险、确认与审计边界。" />
  <section class="security-flow"><article><span>01</span><Workflow /><h2>动态能力</h2><p>每轮根据身份、AAL 与意图计算 Allowed Tool Set。</p></article><article><span>02</span><LockKeyhole /><h2>执行端复核</h2><p>Gateway 再校验 Tool 名称、快照、参数和资源归属。</p></article><article><span>03</span><ShieldCheck /><h2>风险与确认</h2><p>写操作只生成预览，独立确认后才进入最终防线。</p></article><article><span>04</span><DatabaseBackup /><h2>提交与审计</h2><p>Service 事务提交，允许与拒绝都生成脱敏证据。</p></article></section>
  <section class="security-grid"><article class="data-card"><header><div><span class="section-kicker">CURRENT SESSION</span><h2>认证上下文</h2></div><span class="assurance-badge" :class="auth.assuranceLevel.toLowerCase()">{{ auth.assuranceLevel }}</span></header><dl class="details-list"><div><dt>用户</dt><dd>{{ auth.user?.displayName }} / {{ auth.user?.username }}</dd></div><div><dt>角色</dt><dd>{{ auth.user?.role }}</dd></div><div><dt>AAL2 可用</dt><dd>{{ auth.aal2Available ? '是' : '否' }}</dd></div><div><dt>数据边界</dt><dd><CheckCircle2 />仅本人资源</dd></div></dl></article>
  <article class="data-card"><header><div><span class="section-kicker">DETERMINISTIC DATASET</span><h2>Mock Sandbox 重置</h2></div><DatabaseBackup /></header><p>仅管理员且仅 dev/test profile 可执行。重置后固定 seed、日期和摘要保持一致。</p><button class="button danger" :disabled="auth.user?.role !== 'ADMIN' || resetting" @click="reset">{{ resetting ? '正在重置…' : '一键恢复基线' }}</button><small v-if="auth.user?.role !== 'ADMIN'">当前客户角色无重置权限。</small><p v-if="resetError" class="form-alert">{{ resetError }}</p><pre v-if="resetResult">{{ JSON.stringify(resetResult, null, 2) }}</pre></article></section>
  <section class="boundary-banner"><ShieldCheck /><div><h2>明确的信任边界</h2><p>Spring AI Tool 可见性从不作为最终安全边界；fallback 与自动 Tool Calling 均被关闭。</p></div></section>
</div></template>
