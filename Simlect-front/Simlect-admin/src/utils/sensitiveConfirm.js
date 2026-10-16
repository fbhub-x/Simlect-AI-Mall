import { ElMessageBox } from 'element-plus'
import axios from 'axios'
import Message from './Message.js'

const ios26MessageBoxOptions = {
  closeOnClickModal: false,
  distinguishCancelAndClose: true,
  autofocus: false,
  center: true,
}

// 敏感操作确认：先向服务端校验密码换取一次性票据，再执行操作
// （票据 5 分钟有效、绑定管理员与目标操作路径、消费即销毁）
async function exchangeConfirmToken(password, targetPath) {
  const base = import.meta.env.PROD ? '/admin-api' : '/api'
  let res
  try {
    res = await axios.post(
      `${base}/account/confirmPassword`,
      new URLSearchParams({ password, targetPath }),
      {
        withCredentials: true,
        timeout: 10000,
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      },
    )
  } catch (error) {
    throw new Error(error.response?.data?.info || (
      error.code === 'ECONNABORTED'
        ? '密码确认超时，请重试'
        : '密码确认请求失败，请检查网络或稍后重试'
    ))
  }
  if (res.data?.status !== 'success' || !res.data?.data) {
    throw new Error(res.data?.info || '二次确认失败')
  }
  return res.data.data
}

export async function promptAdminConfirmPassword(message = '此操作为敏感操作，请输入管理员密码确认', targetPath = '') {
  // 网关将 /api（开发）或 /admin-api（生产）改写为服务端的 /admin 路径。
  const path = targetPath.trim().replace(/^\/(?:api|admin-api)(?=\/)/, '')
  if (!path.startsWith('/') || path === '/' || path.startsWith('//')) {
    throw new Error('缺少有效的敏感操作接口，请刷新页面后重试')
  }
  const adminPath = path.startsWith('/admin/') ? path : `/admin${path}`
  const { value } = await ElMessageBox.prompt(message, '安全确认', {
    ...ios26MessageBoxOptions,
    inputType: 'password',
    inputPlaceholder: '管理员密码',
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputValidator: (value) => !!value?.trim() || '请输入管理员密码',
  })
  if (!value) {
    throw new Error('请输入管理员密码')
  }
  // 校验密码并换取一次性确认票据（绑定目标操作路径）
  return exchangeConfirmToken(value, adminPath)
}


export const ConfirmSensitive = async ({ message, url, okfun, targetPath }) => {
  try {
    await ElMessageBox.confirm(message, '提示', {
      ...ios26MessageBoxOptions,
      confirmButtonText: '继续',
      cancelButtonText: '取消',
    })
    const confirmToken = await promptAdminConfirmPassword(message, targetPath || url || '')
    if (okfun) {
      await okfun(confirmToken)
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    Message.error(error?.message || '操作失败，请重试')
  }
}

export { ConfirmSensitive as default }
