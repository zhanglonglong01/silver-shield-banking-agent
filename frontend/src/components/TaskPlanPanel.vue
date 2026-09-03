<script setup lang="ts">
import { Check, Circle, OctagonAlert } from 'lucide-vue-next'
import type { TaskPlan } from '@/types/api'
import { reasonLabel } from '@/utils/format'
defineProps<{ plan: TaskPlan }>()
</script>

<template>
  <section class="plan-panel" aria-label="任务执行计划">
    <header><div><span class="section-kicker">受控任务计划</span><h2>{{ plan.goal }}</h2></div><span class="status-pill">{{ plan.status }}</span></header>
    <ol class="plan-steps">
      <li v-for="step in plan.steps" :key="step.stepId" :class="step.status.toLowerCase()">
        <span class="step-icon">
          <Check v-if="step.status === 'COMPLETED'" :size="15" />
          <OctagonAlert v-else-if="step.status === 'FAILED'" :size="15" />
          <Circle v-else :size="12" />
        </span>
        <div><strong>{{ step.tool.name }}</strong><small>{{ step.accessMode }} · {{ reasonLabel(step.reasonCode) }}</small></div>
      </li>
    </ol>
  </section>
</template>
