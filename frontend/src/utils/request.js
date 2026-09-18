import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
    baseURL: '/api', // 这里对应 vite.config.js 里的代理
    timeout: 10000
})

// 请求拦截器
service.interceptors.request.use(config => {
    const token = sessionStorage.getItem('token')
    if (token) config.headers['Authorization'] = `Bearer ${token}`
    return config
})

// 响应拦截器
service.interceptors.response.use(
    res => {
        // 假设后端统一返回 { code: 200, msg: '...', data: ... }
        if (res.data.code === 200) {
            return Promise.resolve(res.data)
        } else {
            ElMessage.error(res.data.msg || '请求失败')
            return Promise.reject(new Error(res.data.msg || '请求失败'))
        }
    },
    err => {
        ElMessage.error(err.message || '网络异常')
        return Promise.reject(err)
    }
)

export default service
