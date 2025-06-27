<template>
  <div class="terminal-container" ref="terminalContainerRef">
    <!-- 固定系统消息区域 -->
    <div class="system-message-bar" :class="systemMessage?.type || 'info'">
      <div class="message-content">
        <el-icon class="message-icon">
          <component :is="getMessageIcon(systemMessage?.type || 'info')" />
        </el-icon>
        <span class="message-text">
          {{ systemMessage?.text || '终端已就绪' }}
        </span>
      </div>
    </div>

    <!-- 标签栏和菜单 -->
    <div class="tabs-header">
      <!-- 简单菜单 -->
      <div class="simple-menu">
        <el-button 
          type="primary" 
          size="small" 
          @click="menuVisible = !menuVisible"
        >
          <el-icon><Plus /></el-icon>
          菜单
        </el-button>
        
        <!-- 简单浮动菜单 -->
        <div v-show="menuVisible" class="floating-menu">
          <div class="menu-option" @click="createNewTab(); menuVisible = false">
            新建终端标签
          </div>
          <div class="menu-option" @click="checkStatus(); menuVisible = false">
            状态检查
          </div>
          <div class="menu-option" @click="copyProjectPath(); menuVisible = false">
            复制项目路径
          </div>
        </div>
      </div>
      
      <!-- 全屏遮罩层，用于捕获点击事件关闭菜单 -->
      <div 
        v-show="menuVisible" 
        class="menu-overlay"
        @click="menuVisible = false"
        @mousedown="menuVisible = false"
      ></div>

      <!-- 标签栏 -->
      <div class="tabs-container" v-if="tabs.length > 0">
        <div class="tabs-wrapper" ref="tabsWrapperRef">
          <div 
            v-for="tab in tabs" 
            :key="tab.id"
            class="tab-item"
            :class="{ active: tab.id === activeTabId }"
            :data-tab-id="tab.id"
            @click="switchTab(tab.id)"
          >
            <span class="tab-title">{{ tab.title }}</span>
            <el-button 
              v-if="tabs.length > 1"
              type="text" 
              size="small" 
              class="tab-close-btn"
              @click.stop="closeTab(tab.id)"
            >
              ×
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 终端内容区域 -->
    <div class="terminal-wrapper" v-if="activeTab">
      <div 
        v-for="tab in tabs"
        :key="tab.id"
        class="terminal-content" 
        :ref="(el) => setTerminalRef(tab.id, el)"
        :style="{ display: tab.id === activeTabId ? 'block' : 'none' }"
      >
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
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElButton, ElIcon } from 'element-plus'
import { Refresh, RefreshRight, DocumentCopy, Plus, SuccessFilled, InfoFilled, WarningFilled, CircleCloseFilled, ArrowDown } from '@element-plus/icons-vue'
import { Terminal } from 'xterm'
import { FitAddon } from '@xterm/addon-fit'
import 'xterm/css/xterm.css'

// 标签接口定义
interface TerminalTab {
  id: string
  title: string
  terminalId: number | null
  terminal: Terminal | null
  fitAddon: FitAddon | null
  containerRef: HTMLElement | null
}

// 响应式数据
const tabs = ref<TerminalTab[]>([])
const activeTabId = ref<string | null>(null)

const isCopyingPath = ref<boolean>(false)

// 简单菜单状态
const menuVisible = ref<boolean>(false)

// 模板引用
const terminalContainerRef = ref<HTMLElement | null>(null)
const tabsWrapperRef = ref<HTMLElement | null>(null)

// 终端ID计数器，确保每个终端都有唯一ID
let terminalIdCounter = 0

// 计算属性
const activeTab = computed(() => tabs.value.find(tab => tab.id === activeTabId.value))

// 系统消息
const systemMessage = ref<{
  text: string
  type: 'info' | 'success' | 'warning' | 'error'
  timestamp: number
} | null>(null)

// 检查是否在 Electron 环境中
const isElectron = typeof window !== 'undefined' && window.require

// 显示系统消息
const showSystemMessage = (message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info') => {
  const timestamp = Date.now()
  systemMessage.value = {
    text: message,
    type,
    timestamp
  }
  
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

// 设置终端容器引用
const setTerminalRef = (tabId: string, el: any) => {
  const tab = tabs.value.find(t => t.id === tabId)
  if (tab) {
    tab.containerRef = el as HTMLElement
  }
}

// 生成唯一标签ID
const generateTabId = () => {
  return 'tab_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

// 生成唯一终端ID
const generateTerminalId = () => {
  return ++terminalIdCounter
}

// 创建新标签
const createNewTab = async () => {
  const tabId = generateTabId()
  const terminalId = generateTerminalId()
  
  const newTab: TerminalTab = {
    id: tabId,
    title: `终端 ${terminalId}`,
    terminalId: null, // 这里先设为null，创建成功后会更新
    terminal: null,
    fitAddon: null,
    containerRef: null
  }
  
  tabs.value.push(newTab)
  activeTabId.value = tabId
  
  // 等待DOM更新后初始化终端
  await nextTick()
  
  await initializeTerminalForTab(newTab)
  
  // 终端初始化完成后再滚动到最右边
  scrollToEnd()
}

// 切换标签
const switchTab = (tabId: string) => {
  activeTabId.value = tabId
  
  // 滚动到当前标签
  scrollToActiveTab(tabId)
  
  // 切换后调整当前标签的终端尺寸并确保焦点
  nextTick(() => {
    const tab = tabs.value.find(t => t.id === tabId)
    if (tab && tab.fitAddon && tab.terminal) {
      setTimeout(() => {
        tab.fitAddon?.fit()
        // 确保新标签的终端获得焦点，这对交互式输入至关重要
        tab.terminal?.focus()
        // 关键：切换标签后也要同步 PTY 尺寸
        if (isElectron && tab.terminalId && tab.terminal) {
          const { ipcRenderer } = window.require('electron')
          ipcRenderer.invoke('terminal-resize', tab.terminalId, tab.terminal.cols, tab.terminal.rows)
        }
      }, 50)
    }
  })
}

// 滚动到指定标签
const scrollToActiveTab = (tabId: string) => {
  nextTick(() => {
    const tabsWrapper = document.querySelector('.tabs-wrapper') as HTMLElement
    const activeTabElement = document.querySelector(`[data-tab-id="${tabId}"]`) as HTMLElement
    
    if (tabsWrapper && activeTabElement) {
      const wrapperRect = tabsWrapper.getBoundingClientRect()
      const tabRect = activeTabElement.getBoundingClientRect()
      
      // 计算标签相对于容器的位置
      const tabOffsetLeft = activeTabElement.offsetLeft
      const tabWidth = activeTabElement.offsetWidth
      
      // 如果标签在可视区域外，滚动到合适位置
      if (tabOffsetLeft < tabsWrapper.scrollLeft) {
        // 标签在左边，滚动到左边
        tabsWrapper.scrollTo({
          left: tabOffsetLeft - 20,
          behavior: 'smooth'
        })
      } else if (tabOffsetLeft + tabWidth > tabsWrapper.scrollLeft + wrapperRect.width) {
        // 标签在右边，滚动到最右边显示新标签
        tabsWrapper.scrollTo({
          left: tabOffsetLeft + tabWidth - wrapperRect.width + 20,
          behavior: 'smooth'
        })
      }
    }
  })
}

// 滚动到最右边（用于新建标签）
const scrollToEnd = () => {
  nextTick(() => {
    setTimeout(() => {
      console.log('[Terminal Debug] tabsWrapperRef.value:', !!tabsWrapperRef.value)
      
      if (tabsWrapperRef.value) {
        const wrapper = tabsWrapperRef.value
        console.log('[Terminal Debug] Scrolling to end, scrollWidth:', wrapper.scrollWidth, 'clientWidth:', wrapper.clientWidth)
        
        // 只有当内容超出容器宽度时才滚动
        if (wrapper.scrollWidth > wrapper.clientWidth) {
          wrapper.scrollTo({
            left: wrapper.scrollWidth,
            behavior: 'smooth'
          })
          console.log('[Terminal Debug] Scroll executed')
        } else {
          console.log('[Terminal Debug] No scroll needed, content fits in container')
        }
      } else {
        console.log('[Terminal Debug] tabsWrapperRef is null')
      }
    }, 200)
  })
}

// 关闭标签
const closeTab = async (tabId: string) => {
  const tabIndex = tabs.value.findIndex(t => t.id === tabId)
  if (tabIndex === -1) return
  
  const tab = tabs.value[tabIndex]
  
  // 销毁终端
  if (tab.terminalId && isElectron) {
    try {
      const { ipcRenderer } = window.require('electron')
      await ipcRenderer.invoke('terminal-destroy', tab.terminalId)
    } catch (error: any) {
      console.warn('[Terminal] Error destroying terminal:', error)
    }
  }
  
  // 清理终端实例
  if (tab.terminal) {
    tab.terminal.dispose()
  }
  

  
  // 从标签列表中移除
  tabs.value.splice(tabIndex, 1)
  
  // 如果关闭的是当前活跃标签，切换到其他标签
  if (activeTabId.value === tabId) {
    if (tabs.value.length > 0) {
      // 优先切换到相邻的标签
      const newActiveIndex = Math.min(tabIndex, tabs.value.length - 1)
      activeTabId.value = tabs.value[newActiveIndex].id
    } else {
      activeTabId.value = null
      // 如果没有标签了，自动创建一个新标签
      setTimeout(() => {
        createNewTab()
      }, 100)
    }
  }
}

// 为特定标签初始化终端
const initializeTerminalForTab = async (tab: TerminalTab) => {
  if (!tab.containerRef) {
    return
  }

  try {
    await nextTick()
    await new Promise(resolve => requestAnimationFrame(resolve))
    
    if (isElectron) {
      const { ipcRenderer } = window.require('electron')
      
      // 获取项目路径作为工作目录
      let projectPath = null
      try {
        const pathResult = await ipcRenderer.invoke('get-project-path')
        if (pathResult && pathResult.success) {
          projectPath = pathResult.projectPath
        }
      } catch (error) {
        console.warn('[Terminal] Failed to get project path, using default cwd')
      }
      
      // 先获取实际的终端尺寸
      const containerRect = tab.containerRef.getBoundingClientRect()
      const cols = Math.floor(containerRect.width / 9) || 80  // 假设字符宽度约9px
      const rows = Math.floor(containerRect.height / 17) || 24 // 假设字符高度约17px
      
      const result = await ipcRenderer.invoke('terminal-create', {
        cols: cols,
        rows: rows,
        cwd: projectPath || undefined
      })
      
      if (result && result.success && result.terminalId) {
        tab.terminalId = result.terminalId
        
        await initializeXtermForTab(tab)
        await setupTerminalListenersForTab(tab)
        
        // 确保新创建的终端获得焦点
        setTimeout(() => {
          if (tab.terminal) {
            tab.terminal.focus()
          }
        }, 100)
        
        showSystemMessage(`标签 "${tab.title}" 终端连接成功 (PTY ID: ${tab.terminalId})`, 'success')
        
        // 如果这是新创建的标签，滚动到最右边
        if (tab.id === activeTabId.value) {
          setTimeout(() => scrollToEnd(), 100)
        }
        
      } else {
        throw new Error(result?.error || 'Failed to create terminal')
      }
    } else {
      showSystemMessage('开发模式：无法创建真实终端', 'warning')
    }
  } catch (error: any) {
    console.error('[Terminal] Failed to create terminal for tab:', error)
    showSystemMessage(`无法创建终端 "${tab.title}": ` + error.message, 'error')
  }
}

// 为特定标签初始化 xterm.js
const initializeXtermForTab = async (tab: TerminalTab) => {
  if (!tab.containerRef) {
    return
  }

  try {
    // 使用实际的容器尺寸来计算终端尺寸
    const containerRect = tab.containerRef.getBoundingClientRect()
    const cols = Math.floor(containerRect.width / 9) || 80
    const rows = Math.floor(containerRect.height / 17) || 24
    
    tab.terminal = new Terminal({
      theme: {
        background: '#1e1e1e',
        foreground: '#ffffff',
        cursor: '#ffffff',
        selectionBackground: '#3e3e3e'
      },
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      fontSize: 14,
      cursorBlink: true,
      allowTransparency: false,
      cols: cols,
      rows: rows,
      scrollback: 10000,
      rightClickSelectsWord: true,
      macOptionIsMeta: true,
      // 关键：必须设为 false，让 PTY 处理换行转换
      convertEol: false,
      disableStdin: false,
      cursorStyle: 'block',
      logLevel: 'info',
      altClickMovesCursor: false,
      allowProposedApi: true
    })

    tab.fitAddon = new FitAddon()
    tab.terminal.loadAddon(tab.fitAddon)

    tab.terminal.open(tab.containerRef)
    
    await nextTick()
    
    setTimeout(() => {
      if (tab.fitAddon && tab.terminal) {
        tab.fitAddon.fit()
        // 同步 PTY 尺寸
        if (isElectron && tab.terminalId) {
          const { ipcRenderer } = window.require('electron')
          ipcRenderer.invoke('terminal-resize', tab.terminalId, tab.terminal.cols, tab.terminal.rows)
        }
      }
    }, 50)
    
    setTimeout(() => {
      if (tab.fitAddon && tab.terminal) {
        tab.fitAddon.fit()
        // 再次同步 PTY 尺寸
        if (isElectron && tab.terminalId) {
          const { ipcRenderer } = window.require('electron')
          ipcRenderer.invoke('terminal-resize', tab.terminalId, tab.terminal.cols, tab.terminal.rows)
        }
      }
    }, 200)

    // 确保终端获得焦点，这对交互式输入至关重要
    tab.terminal.focus()
    
    tab.terminal.onData((data) => {
      if (isElectron && tab.terminalId) {
        const { ipcRenderer } = window.require('electron')
        ipcRenderer.invoke('terminal-write', tab.terminalId, data).catch((error: any) => {
          console.error('[Terminal] Write error:', error)
        })
      }
    })
    
    // 添加点击事件监听，确保点击终端时能获得焦点
    tab.containerRef.addEventListener('click', () => {
      if (tab.terminal) {
        tab.terminal.focus()
      }
    })
    


  } catch (error: any) {
    console.error('[Terminal] Failed to initialize xterm.js for tab:', error)
    showSystemMessage(`标签 "${tab.title}" 终端界面初始化失败`, 'error')
  }
}

// 全局监听器状态
let globalListenersSetup = false

// 全局数据监听器
const setupGlobalListeners = async () => {
  if (!isElectron || globalListenersSetup) return

  const { ipcRenderer } = window.require('electron')

  // 全局终端数据监听器
  const globalTerminalDataListener = (event: any, data: any) => {
    // 找到对应的标签并写入数据
    const tab = tabs.value.find(t => t.terminalId === data.terminalId)
    if (tab && tab.terminal) {
      tab.terminal.write(data.data)
    }
  }

  // 全局终端退出监听器
  const globalTerminalExitListener = (event: any, data: any) => {
    // 找到对应的标签并显示退出消息
    const tab = tabs.value.find(t => t.terminalId === data.terminalId)
    if (tab) {
      showSystemMessage(`标签 "${tab.title}" 终端进程已退出 (代码: ${data.code})`, 'warning')
    }
  }

  ipcRenderer.on('terminal-data', globalTerminalDataListener)
  ipcRenderer.on('terminal-exit', globalTerminalExitListener)

  try {
    await ipcRenderer.invoke('terminal-setup-listener')
    globalListenersSetup = true
  } catch (error: any) {
    console.error('[Terminal] Failed to setup global listeners:', error)
    showSystemMessage('设置终端监听器失败', 'error')
  }
}

// 为特定标签设置终端数据监听（简化版）
const setupTerminalListenersForTab = async (tab: TerminalTab) => {
  // 确保全局监听器已设置
  await setupGlobalListeners()
}

// 处理窗口大小变化
const handleResize = () => {
  console.log('[Terminal Debug] handleResize triggered')
  
  if (activeTab.value && activeTab.value.fitAddon && activeTab.value.terminal) {
    console.log('[Terminal Debug] Active tab found, starting resize process')
    
    setTimeout(() => {
      if (activeTab.value && activeTab.value.fitAddon && activeTab.value.terminal) {
        const oldSize = `${activeTab.value.terminal.cols}x${activeTab.value.terminal.rows}`
        const wasAtBottom = activeTab.value.terminal.buffer.active.viewportY + activeTab.value.terminal.rows >= activeTab.value.terminal.buffer.active.length
        
        activeTab.value.fitAddon.fit()
        
        setTimeout(() => {
          if (activeTab.value && activeTab.value.fitAddon && activeTab.value.terminal) {
            activeTab.value.fitAddon.fit()
            
            const newSize = `${activeTab.value.terminal.cols}x${activeTab.value.terminal.rows}`
            console.log(`[Terminal Debug] Size changed from ${oldSize} to ${newSize}`)
            
            if (wasAtBottom) {
              activeTab.value.terminal.scrollToBottom()
            }
            
            if (isElectron && activeTab.value.terminalId) {
              const { ipcRenderer } = window.require('electron')
              ipcRenderer.invoke('terminal-resize', activeTab.value.terminalId, activeTab.value.terminal.cols, activeTab.value.terminal.rows)
              console.log(`[Terminal Debug] PTY resize sent: ${newSize}`)
            }
          }
        }, 50)
      }
    }, 100)
  } else {
    console.log('[Terminal Debug] No active tab or terminal found')
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
      } else {
        showSystemMessage('PTY 服务器未运行', 'error')
      }
    }
  } catch (error: any) {
    console.error('[Terminal] Failed to check status:', error)
  }
}

// 复制项目路径
const copyProjectPath = async () => {
  if (isCopyingPath.value) {
    return
  }
  
  try {
    isCopyingPath.value = true
    
    if (isElectron) {
      const { ipcRenderer } = window.require('electron')
      
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Operation timeout')), 3000)
      })
      
      const result = await Promise.race([
        ipcRenderer.invoke('get-project-path'),
        timeoutPromise
      ])
      
      if (result && result.success) {
        try {
          if (navigator.clipboard && navigator.clipboard.writeText) {
            await navigator.clipboard.writeText(result.projectPath)
          } else {
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

// 使用遮罩层来处理菜单关闭，不需要复杂的全局事件监听了

// 组件挂载时初始化
onMounted(async () => {
  // 通知主进程面板已打开，启动PTY服务器
  if (isElectron) {
    try {
      const { ipcRenderer } = window.require('electron')
      const result = await ipcRenderer.invoke('terminal-panel-opened')
      if (result && result.success) {
        showSystemMessage(result.message || 'PTY服务器已启动', 'success')
      } else {
        showSystemMessage('启动PTY服务器失败: ' + (result?.error || '未知错误'), 'error')
      }
    } catch (error: any) {
      console.error('[Terminal] Failed to start PTY server on panel open:', error)
      showSystemMessage('启动PTY服务器失败: ' + error.message, 'error')
    }
  }
  
  // 自动创建第一个标签
  await createNewTab()
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
  console.log('[Terminal Debug] Window resize listener added')
  
  // 等待 DOM 完全渲染后再设置容器监听器
  await nextTick()
  setTimeout(() => {
    // 使用 Vue 模板引用获取容器
    console.log('[Terminal Debug] Terminal container ref:', !!terminalContainerRef.value)
    
    if (terminalContainerRef.value && window.ResizeObserver) {
      const panelResizeObserver = new ResizeObserver((entries) => {
        const rect = entries[0]?.contentRect
        console.log('[Terminal Debug] Panel container resized:', `${rect?.width}x${rect?.height}`)
        handleResize()
      })
      panelResizeObserver.observe(terminalContainerRef.value)
      console.log('[Terminal Debug] ResizeObserver attached to terminal container')
      
      // 保存引用以便清理
      ;(window as any).__panelResizeObserver = panelResizeObserver
    } else {
      console.log('[Terminal Debug] ResizeObserver not available or container ref not found')
    }
  }, 500) // 延迟 500ms 确保 DOM 完全渲染
})

// 组件卸载时清理
onUnmounted(async () => {
  // 通知主进程面板已关闭，停止PTY服务器
  if (isElectron) {
    try {
      const { ipcRenderer } = window.require('electron')
      
      // 先销毁所有终端
      for (const tab of tabs.value) {
        if (tab.terminalId) {
          await ipcRenderer.invoke('terminal-destroy', tab.terminalId)
        }
      }
      
      // 通知面板关闭
      const result = await ipcRenderer.invoke('terminal-panel-closed')
      if (result && result.success) {
        // PTY server stopped successfully
      }
    } catch (error: any) {
      console.error('[Terminal] Failed to stop PTY server on panel close:', error)
    }
  }
  
  // 清理所有标签
  tabs.value.forEach(tab => {
    if (tab.terminal) {
      tab.terminal.dispose()
    }

  })
  
  window.removeEventListener('resize', handleResize)
  
  // 清理面板容器监听器
  if ((window as any).__panelResizeObserver) {
    ;(window as any).__panelResizeObserver.disconnect()
    delete (window as any).__panelResizeObserver
  }
})
</script>

<style>
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

/* 标签栏和菜单容器 */
.tabs-header {
  display: flex;
  align-items: center;
  background-color: #2d2d2d;
  border-bottom: 1px solid #3e3e3e;
  flex-shrink: 0;
  position: relative;
  height: 36px; /* 设置固定高度，比默认高度小约20% */
}

/* 简单菜单样式 */
.simple-menu {
  position: relative;
  padding: 6px 12px; /* 减少内边距配合新高度 */
  border-right: 1px solid #3e3e3e;
  flex-shrink: 0;
}

.floating-menu {
  position: absolute;
  top: 100%;
  left: 0;
  background-color: #2d2d2d;
  border: 1px solid #3e3e3e;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  z-index: 9999;
  width: max-content; /* 根据内容自动调整宽度 */
  min-width: 120px;
  padding: 4px 0;
}

.menu-option {
  padding: 8px 16px;
  color: #ffffff;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 14px;
  white-space: nowrap;
  border-bottom: 1px solid #3e3e3e; /* 添加分割线 */
}

.menu-option:last-child {
  border-bottom: none; /* 最后一个选项不显示分割线 */
}

.menu-option:hover {
  background-color: #3e3e3e;
}

/* 全屏遮罩层 */
.menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9998; /* 比菜单低一层 */
  background-color: transparent;
  cursor: default;
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

/* 标签栏样式 */
.tabs-container {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.tabs-wrapper {
  display: flex;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none; /* Firefox */
  scroll-behavior: smooth;
}

/* 隐藏滚动条 - Webkit */
.tabs-wrapper::-webkit-scrollbar {
  display: none;
}

.tab-item {
  display: flex;
  align-items: center;
  padding: 6px 12px; /* 从 8px 减少到 6px，减少约 25% */
  background-color: #3e3e3e;
  border-right: 1px solid #2d2d2d;
  cursor: pointer;
  user-select: none;
  min-width: 120px;
  max-width: 200px;
  transition: background-color 0.2s;
}

.tab-item:hover {
  background-color: #4e4e4e;
}

.tab-item.active {
  background-color: #1e1e1e;
  border-bottom: 2px solid #409eff;
}

.tab-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  color: #ffffff;
}

.tab-close-btn {
  color: #ffffff !important;
  font-size: 16px;
  font-weight: bold;
  padding: 0 !important;
  min-width: 20px !important;
  height: 20px !important;
  margin-left: 8px;
  border-radius: 50%;
}

.tab-close-btn:hover {
  background-color: rgba(255, 255, 255, 0.2) !important;
}

.terminal-wrapper {
  flex: 1;
  position: relative;
  overflow: hidden;
  padding: 0;
  margin: 0;
  min-height: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.terminal-content {
  flex: 1;
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
  padding: 8px; /* 添加内边距，上下左右各8px */
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
</style> 