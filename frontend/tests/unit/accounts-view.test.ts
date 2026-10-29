import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AccountsView from '@/views/AccountsView.vue'
import { apiRequest } from '@/services/http'

vi.mock('@/services/http', () => ({ apiRequest: vi.fn() }))
const request = vi.mocked(apiRequest)
const account = (id: string, balance = '83316.43') => ({
  id, alias: id === 'daily' ? '日常账户' : '安心储蓄', accountMasked: '**** 8899',
  availableBalance: balance, currency: 'CNY', dailyTransferLimit: '50000.00', status: 'ACTIVE'
})
const transfer = { id: 'tx-88', bookingTime: '2026-10-29T05:14:49Z', direction: 'OUT',
  amount: '88.00', currency: 'CNY', categoryCode: 'TRANSFER', merchantName: '张伟（虚构）',
  description: '转账', status: 'POSTED', balanceAfter: '83316.43' }

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-10-29T12:00:00Z'))
  request.mockReset()
})
afterEach(() => vi.useRealTimers())

describe('account transactions', () => {
  it('includes today and displays the newly posted transfer by default', async () => {
    request.mockResolvedValueOnce([account('daily')]).mockResolvedValueOnce([transfer])
    const view = mount(AccountsView)
    await flushPromises()
    expect(request).toHaveBeenLastCalledWith('/api/v1/accounts/daily/transactions?from=2025-10-29&to=2026-10-29&limit=20')
    expect(view.text()).toContain('张伟（虚构）')
    expect(view.text()).toContain('-¥88.00')
    expect(view.text()).toContain('POSTED')
    expect(view.text()).toContain('83,316.43')
    view.unmount()
  })

  it('supports the fixed baseline separately from current transactions', async () => {
    request.mockResolvedValueOnce([account('daily')]).mockResolvedValueOnce([transfer]).mockResolvedValueOnce([])
    const view = mount(AccountsView)
    await flushPromises()
    await view.get('select').setValue('baseline')
    await flushPromises()
    expect(request).toHaveBeenLastCalledWith('/api/v1/accounts/daily/transactions?from=2026-08-01&to=2026-08-31&limit=20')
    expect(view.text()).toContain('基线账单')
    expect(view.text()).toContain('此期间暂无交易')
    expect(view.text()).not.toContain('张伟（虚构）')
    view.unmount()
  })

  it('refreshes balances and transactions without switching away from the selected account', async () => {
    request.mockResolvedValueOnce([account('savings'), account('daily')]).mockResolvedValueOnce([])
    const view = mount(AccountsView)
    await flushPromises()
    request.mockResolvedValueOnce([transfer])
    await view.findAll('.account-card')[1]!.trigger('click')
    await flushPromises()
    request.mockResolvedValueOnce([account('savings'), account('daily', '83228.43')]).mockResolvedValueOnce([transfer])
    await view.get('button').trigger('click')
    await flushPromises()
    expect(request).toHaveBeenLastCalledWith(expect.stringContaining('/accounts/daily/transactions?'))
    expect(view.text()).toContain('83,228.43')
    view.unmount()
  })

  it('clears previous rows and displays a failure when a range request fails', async () => {
    request.mockResolvedValueOnce([account('daily')]).mockResolvedValueOnce([transfer]).mockRejectedValueOnce(new Error('读取交易失败'))
    const view = mount(AccountsView)
    await flushPromises()
    await view.get('select').setValue('baseline')
    await flushPromises()
    expect(view.text()).toContain('读取交易失败')
    expect(view.text()).not.toContain('张伟（虚构）')
    view.unmount()
  })

  it('does not show stale rows when account responses arrive out of order', async () => {
    let resolveOld: (value: unknown) => void = () => {}
    request.mockResolvedValueOnce([account('savings'), account('daily')])
      .mockImplementationOnce(() => new Promise(resolve => { resolveOld = resolve }))
    const view = mount(AccountsView)
    await flushPromises()
    resolveOld([])
    await flushPromises()
    request.mockImplementationOnce(() => new Promise(resolve => { resolveOld = resolve }))
    await view.findAll('.account-card')[0]!.trigger('click')
    request.mockResolvedValueOnce([transfer])
    await view.findAll('.account-card')[1]!.trigger('click')
    await flushPromises()
    resolveOld([{ ...transfer, merchantName: '旧账户流水' }])
    await flushPromises()
    expect(view.text()).toContain('张伟（虚构）')
    expect(view.text()).not.toContain('旧账户流水')
    view.unmount()
  })
})
