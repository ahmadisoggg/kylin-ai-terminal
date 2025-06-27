<template>
  <div class="terminal-container">
    <div class="terminal-header">
      <div class="terminal-controls">
        <el-button 
          type="info" 
          size="small" 
          @click="checkStatus"
        >
          <el-icon><RefreshRight /></el-icon>
          状态检查
        </el-button>
        <el-button 
          type="warning" 
          size="small" 
          @click="restartServer"
          :loading="isRestarting"
        >
          <el-icon><Refresh /></el-icon>
          重启服务
        </el-button>
        <el-button 
          type="success" 
          size="small" 
          @click="copyProjectPath"
          :loading="isCopyingPath"
          :disabled="isCopyingPath"
        >
          <el-icon><DocumentCopy /></el-icon>
          {{ isCopyingPath ? '复制中...' : '复制项目路径' }}
        </el-button>
        <el-button 
          type="primary" 
          size="small" 
          @click="forceResize"
        >
          <el-icon><FullScreen /></el-icon>
          调整尺寸
        </el-button>
      </div>
    </div>

    <!-- 系统消息栏 -->
    <div v-if="systemMessage" class="system-message-bar" :class="systemMessage.type">
      <div class="message-content">
        <el-icon class="message-icon">
          <component :is="getMessageIcon(systemMessage.type)" />
        </el-icon>
        <span class="message-text">{{ systemMessage.text }}</span>
      </div>
      <el-button 
        type="text" 
        size="small" 
        class="close-btn"
        @click="clearSystemMessage"
      >
        ×
      </el-button>
    </div>

    <div class="terminal-wrapper" v-if="terminalId">
      <div class="terminal-content" ref="terminalContainer">
        <!-- xterm.js 终端将在这里渲染 -->
      </div>
    </div>
    
    <div class="terminal-loading" v-else>
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <p>准备启动终端...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElButton, ElIcon } from 'element-plus'
import { Refresh, RefreshRight, DocumentCopy, FullScreen, SuccessFilled, InfoFilled, WarningFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { Terminal } from 'xterm'
import { FitAddon } from '@xterm/addon-fit'
import 'xterm/css/xterm.css'

// 响应式数据
const terminalId = ref<number | null>(null)
const terminalContainer = ref<HTMLElement>()
const isRestarting = ref<boolean>(false)
const isCopyingPath = ref<boolean>(false)

// 系统消息
const systemMessage = ref<{
  text: string
  type: 'info' | 'success' | 'warning' | 'error'
  timestamp: number
} | null>(null)

// xterm.js 实例
let terminal: Terminal | null = null
let fitAddon: FitAddon | null = null

// 检查是否在 Electron 环境中
const isElectron = typeof window !== 'undefined' && window.require

// 显示系统消息（独立的消息栏）
const showSystemMessage = (message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info') => {
  const timestamp = Date.now()
  systemMessage.value = {
    text: message,
    type,
    timestamp
  }
  
  // 自动清除消息（5秒后）
  setTimeout(() => {
    if (systemMessage.value && systemMessage.value.timestamp === timestamp) {
      clearSystemMessage()
    }
  }, 5000)
}

// 清除系统消息
const clearSystemMessage = () => {
  systemMessage.value = null
}

// 获取消息图标
const getMessageIcon = (type: string) => {
  switch (type) {
    case 'success': return SuccessFilled
    case 'warning': return WarningFilled
    case 'error': return CircleCloseFilled
    case 'info':
    default: return InfoFilled
  }
}

// 创建终端
const createTerminal = async () => {
  try {
    
    if (isElectron) {
      const { ipcRenderer } = window.require('electron')
      
      // 设置终端数据监听器
      await ipcRenderer.invoke('terminal-setup-listener')
      
      // 创建终端 - 默认定位到插件工程根目录
      const result = await ipcRenderer.invoke('terminal-create', {
        cols: 80,
        rows: 24
        // cwd 将由主进程自动设置为工程根目录
      })
      
      if (result && result.success && result.terminalId) {
        terminalId.value = result.terminalId
        
        // 初始化 xterm.js
        await nextTick()
        await initializeXterm()
        
        // 设置数据监听
        await setupTerminalListeners()
        
        // 显示系统消息
        showSystemMessage('终端连接成功', 'success')
        
      } else {
        throw new Error(result?.error || 'Failed to create terminal')
      }
    } else {
      // 开发模式
      showSystemMessage('开发模式：无法创建真实终端', 'warning')
    }
  } catch (error: any) {
    console.error('[Terminal] Failed to create terminal:', error)
    showSystemMessage('无法创建终端: ' + error.message, 'error')
  }
}

// 初始化 xterm.js
const initializeXterm = async () => {
  if (!terminalContainer.value) {
    return
  }

  try {
    // 等待容器完全渲染
    await nextTick()
    
    // 等待一帧，确保CSS布局完成
    await new Promise(resolve => requestAnimationFrame(resolve))
    
    // 创建 Terminal 实例 - 使用简单的默认尺寸，后续通过 fit 调整
    terminal = new Terminal({
      theme: {
        background: '#1e1e1e',
        foreground: '#ffffff',
        cursor: '#ffffff',
        selectionBackground: '#3e3e3e'
      },
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      fontSize: 14,
      cursorBlink: true,
      allowTransparency: false, // 禁用透明，避免渲染问题
      cols: 80, // 使用标准默认值
      rows: 24, // 使用标准默认值
      scrollback: 10000, // 滚动缓冲区
      rightClickSelectsWord: true,
      macOptionIsMeta: true,
      convertEol: true,
      disableStdin: false,
      cursorStyle: 'block',
      logLevel: 'warn'
    })

    // 创建 FitAddon
    fitAddon = new FitAddon()
    terminal.loadAddon(fitAddon)

    // 打开终端到容器
    terminal.open(terminalContainer.value)
    
    // 等待终端完全打开
    await nextTick()
    
    // 调整大小以适应容器
    setTimeout(() => {
      if (fitAddon && terminal) {
        fitAddon.fit()
      }
    }, 50)
    
    // 再次调整，确保尺寸正确
    setTimeout(() => {
      if (fitAddon && terminal) {
        fitAddon.fit()
      }
    }, 200)

    // 监听用户输入
    terminal.onData((data) => {
      if (isElectron && terminalId.value) {
        const { ipcRenderer } = window.require('electron')
        ipcRenderer.invoke('terminal-write', terminalId.value, data).catch((error: any) => {
          console.error('[Terminal] Write error:', error)
        })
      }
    })

    // 监听窗口大小变化
    window.addEventListener('resize', handleResize)
    
    // 监听容器大小变化
    if (terminalContainer.value && window.ResizeObserver) {
      const resizeObserver = new ResizeObserver(() => {
        handleResize()
      })
      resizeObserver.observe(terminalContainer.value)
    }

  } catch (error: any) {
    console.error('[Terminal] Failed to initialize xterm.js:', error)
    showSystemMessage('终端界面初始化失败', 'error')
  }
}

// 存储监听器函数的引用，以便后续清理
let terminalDataListener: ((event: any, data: any) => void) | null = null
let terminalExitListener: ((event: any, data: any) => void) | null = null

// 设置终端数据监听
const setupTerminalListeners = async () => {
  if (!isElectron) return

  const { ipcRenderer } = window.require('electron')

  // 先清理旧的监听器
  cleanupListeners()

  // 创建新的监听器函数
  terminalDataListener = (event: any, data: any) => {
    if (data.terminalId === terminalId.value && terminal) {
      terminal.write(data.data)
    }
  }

  terminalExitListener = (event: any, data: any) => {
    if (data.terminalId === terminalId.value) {
      showSystemMessage(`终端进程已退出 (代码: ${data.code})`, 'warning')
    }
  }

  // 设置监听器
  ipcRenderer.on('terminal-data', terminalDataListener)
  ipcRenderer.on('terminal-exit', terminalExitListener)

  // 然后建立消息转发
  try {
    await ipcRenderer.invoke('terminal-setup-listener')
  } catch (error: any) {
    console.error('[Terminal] Failed to setup listener:', error)
    showSystemMessage('设置终端监听器失败', 'error')
  }
}

// 清理监听器
const cleanupListeners = () => {
  if (isElectron) {
    const { ipcRenderer } = window.require('electron')
    
    // 移除特定的监听器
    if (terminalDataListener) {
      ipcRenderer.removeListener('terminal-data', terminalDataListener)
      terminalDataListener = null
    }
    
    if (terminalExitListener) {
      ipcRenderer.removeListener('terminal-exit', terminalExitListener)
      terminalExitListener = null
    }
  }
}

// 处理窗口大小变化
const handleResize = () => {
  if (fitAddon && terminal && terminalId.value && terminalContainer.value) {
    // 延迟调整大小，确保容器大小已更新
          setTimeout(() => {
        if (fitAddon && terminal && terminalContainer.value) {
          // 获取当前滚动位置
          const wasAtBottom = terminal.buffer.active.viewportY + terminal.rows >= terminal.buffer.active.length
          
          // 调整终端尺寸
          fitAddon.fit()
        
        // 再次调整以确保准确
        setTimeout(() => {
          if (fitAddon && terminal) {
            fitAddon.fit()
            
            // 如果之前在底部，调整后也滚动到底部
            if (wasAtBottom) {
              terminal.scrollToBottom()
            }
            
            // 通知后端调整 PTY 大小
            if (isElectron) {
              const { ipcRenderer } = window.require('electron')
              ipcRenderer.invoke('terminal-resize', terminalId.value, terminal.cols, terminal.rows)
            }
          }
        }, 50)
      }
    }, 100)
  }
}

// 检查终端状态
const checkStatus = async () => {
  try {
    if (isElectron) {
      const { ipcRenderer } = window.require('electron')
      const result = await ipcRenderer.invoke('terminal-check-status')
      
      if (result && result.isRunning) {
        showSystemMessage(`PTY 服务器正在运行 (活跃终端: ${result.activeTerminals?.length || 0})`, 'success')
        // ElMessage.success('终端服务器运行正常')
      } else {
        showSystemMessage('PTY 服务器未运行', 'error')
        // ElMessage.error('终端服务器未运行')
      }
    } else {
      // ElMessage.warning('开发模式：无法检查服务器状态')
    }
  } catch (error: any) {
    console.error('[Terminal] Failed to check status:', error)
    // ElMessage.error('状态检查失败: ' + error.message)
  }
}

// 复制项目路径
const copyProjectPath = async () => {
  // 防止重复点击
  if (isCopyingPath.value) {
    return
  }
  
  try {
    isCopyingPath.value = true
    
    if (isElectron) {
      const { ipcRenderer } = window.require('electron')
      
      // 添加超时保护
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Operation timeout')), 3000) // 减少到3秒
      })
      
      const result = await Promise.race([
        ipcRenderer.invoke('get-project-path'),
        timeoutPromise
      ])
      
      if (result && result.success) {
        // 使用更安全的剪贴板操作
        try {
          if (navigator.clipboard && navigator.clipboard.writeText) {
            await navigator.clipboard.writeText(result.projectPath)
          } else {
            // 降级方案：使用传统的复制方法
            const textArea = document.createElement('textarea')
            textArea.value = result.projectPath
            textArea.style.position = 'fixed'
            textArea.style.left = '-999999px'
            textArea.style.top = '-999999px'
            document.body.appendChild(textArea)
            textArea.focus()
            textArea.select()
            document.execCommand('copy')
            document.body.removeChild(textArea)
          }
          
          showSystemMessage(`项目路径已复制: ${result.projectPath}`, 'success')
        } catch (clipboardError: any) {
          console.error('[Terminal] Clipboard error:', clipboardError)
          showSystemMessage('复制到剪贴板失败，但路径已获取', 'warning')
        }
      } else {
        throw new Error(result?.error || 'Failed to get project path')
      }
    } else {
      showSystemMessage('开发模式：无法获取项目路径', 'warning')
    }
  } catch (error: any) {
    console.error('[Terminal] Failed to copy project path:', error)
    showSystemMessage('复制项目路径失败: ' + error.message, 'error')
  } finally {
    isCopyingPath.value = false
  }
}


// 强制调整终端尺寸
const forceResize = () => {
  if (fitAddon && terminal && terminalContainer.value) {
    showSystemMessage('正在调整终端尺寸...', 'info')
    
    // 强制刷新容器样式
    const container = terminalContainer.value
    const originalDisplay = container.style.display
    container.style.display = 'none'
    
    // 强制重排
    container.offsetHeight
    
    container.style.display = originalDisplay
    
    // 多次调整以确保生效
    setTimeout(() => {
      if (fitAddon && terminal) {
        fitAddon.fit()
        setTimeout(() => {
          if (fitAddon && terminal) {
            fitAddon.fit()
            terminal.scrollToBottom()
            showSystemMessage('终端尺寸已调整', 'success')
            
            // 通知后端调整 PTY 大小
            if (isElectron && terminalId.value) {
              const { ipcRenderer } = window.require('electron')
              ipcRenderer.invoke('terminal-resize', terminalId.value, terminal.cols, terminal.rows)
            }
          }
        }, 100)
      }
    }, 50)
  }
}

// 重启服务器
const restartServer = async () => {
  if (isRestarting.value) return
  
  try {
    isRestarting.value = true
    
    if (isElectron) {
      const { ipcRenderer } = window.require('electron')
      
      // 先销毁当前终端
      if (terminalId.value) {
        await destroyTerminal()
      }
      
      // 重启服务器
      const result = await ipcRenderer.invoke('terminal-restart')
      
      if (result && result.success) {
        // 自动创建新终端
        setTimeout(async () => {
          await createTerminal()
        }, 1000)
      } else {
        throw new Error(result?.error || 'Failed to restart server')
      }
    } else {
      showSystemMessage('开发模式：无法重启服务器', 'warning')
    }
  } catch (error: any) {
    console.error('[Terminal] Failed to restart server:', error)
    showSystemMessage('重启服务器失败: ' + error.message, 'error')
  } finally {
    isRestarting.value = false
  }
}

// 清理终端
const cleanupTerminal = () => {
  if (terminal) {
    terminal.dispose()
    terminal = null
  }
  
  if (fitAddon) {
    fitAddon = null
  }
  
  window.removeEventListener('resize', handleResize)
  
  // 清理 IPC 监听器
  cleanupListeners()
}

// 销毁终端
const destroyTerminal = async () => {
  if (terminalId.value && isElectron) {
    try {
      const { ipcRenderer } = window.require('electron')
      await ipcRenderer.invoke('terminal-destroy', terminalId.value)
    } catch (error: any) {
      console.warn('[Terminal] Error destroying terminal:', error)
    }
  }
  
  terminalId.value = null
  cleanupTerminal()
}

// 组件挂载时初始化
onMounted(async () => {
  // 自动创建终端
  await createTerminal()
  
  // 确保在组件完全挂载后调整大小
  await nextTick()
  setTimeout(() => {
    handleResize()
  }, 200)
})

// 组件卸载时清理
onUnmounted(() => {
  destroyTerminal()
})
</script>

<style scoped>
.terminal-container {
  display: flex;
  flex-direction: column;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #1e1e1e;
  color: #ffffff;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.terminal-header {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 15px;
  background-color: #2d2d2d;
  border-bottom: 1px solid #3e3e3e;
  flex-shrink: 0;
}

.terminal-controls {
  display: flex;
  gap: 8px;
}

.system-message-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 15px;
  border-bottom: 1px solid #3e3e3e;
  flex-shrink: 0;
  animation: slideDown 0.3s ease-out;
}

.system-message-bar.success {
  background-color: #1f2d1f;
  border-left: 4px solid #67c23a;
}

.system-message-bar.info {
  background-color: #1f2a2d;
  border-left: 4px solid #409eff;
}

.system-message-bar.warning {
  background-color: #2d2a1f;
  border-left: 4px solid #e6a23c;
}

.system-message-bar.error {
  background-color: #2d1f1f;
  border-left: 4px solid #f56c6c;
}

.message-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.message-icon {
  font-size: 16px;
}

.system-message-bar.success .message-icon {
  color: #67c23a;
}

.system-message-bar.info .message-icon {
  color: #409eff;
}

.system-message-bar.warning .message-icon {
  color: #e6a23c;
}

.system-message-bar.error .message-icon {
  color: #f56c6c;
}

.message-text {
  color: #ffffff;
  font-size: 14px;
}

.close-btn {
  color: #ffffff !important;
  font-size: 18px;
  font-weight: bold;
  padding: 0 !important;
  min-width: 24px !important;
  height: 24px !important;
}

.close-btn:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.terminal-wrapper {
  flex: 1;
  position: relative;
  overflow: hidden; /* 隐藏外层滚动，让xterm处理 */
  padding: 0;
  margin: 0;
  min-height: 0; /* 允许flex收缩 */
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.terminal-content {
  flex: 1;
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden; /* 让xterm.js控制滚动 */
  box-sizing: border-box;
}

.terminal-loading {
  flex: 1;
  position: relative;
  background-color: #000000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.loading-content {
  text-align: center;
  color: #ffffff;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #3e3e3e;
  border-top: 4px solid #ffffff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 深色主题样式 */
:deep(.el-alert) {
  background-color: #2d2d2d;
  border: 1px solid #3e3e3e;
}

:deep(.el-alert__title) {
  color: #ffffff;
}

:deep(.el-button) {
  background-color: #3e3e3e;
  border-color: #5e5e5e;
  color: #ffffff;
}

:deep(.el-button:hover) {
  background-color: #4e4e4e;
  border-color: #6e6e6e;
}

/* xterm.js 样式覆盖 - 简化版本 */
:deep(.xterm) {
  width: 100% !important;
  height: 100% !important;
  padding: 0;
  margin: 0;
  box-sizing: border-box;
}

:deep(.xterm-viewport) {
  background-color: transparent !important;
  /* 移除所有强制的滚动设置，让xterm.js自己处理 */
}

:deep(.xterm-screen) {
  /* 保持默认样式 */
}

/* 自定义滚动条样式 */
:deep(.xterm-viewport)::-webkit-scrollbar {
  width: 8px;
}

:deep(.xterm-viewport)::-webkit-scrollbar-track {
  background: #2d2d2d;
  border-radius: 4px;
}

:deep(.xterm-viewport)::-webkit-scrollbar-thumb {
  background: #5e5e5e;
  border-radius: 4px;
}

:deep(.xterm-viewport)::-webkit-scrollbar-thumb:hover {
  background: #6e6e6e;
}

/* 强制显示滚动条 */
:deep(.xterm-viewport) {
  scrollbar-gutter: stable !important;
}

:deep(.xterm-screen) {
  text-align: left;
}

:deep(.xterm-rows) {
  text-align: left;
}

:deep(.xterm-helper-textarea) {
  text-align: left;
}
</style> 