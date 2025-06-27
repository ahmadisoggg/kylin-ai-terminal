import { name } from '../../package.json' with { type: 'json' };
import { spawn, ChildProcess } from 'child_process';
import * as path from 'path';

// 主进程使用 node-pty 子进程架构避免 MODULE_VERSION 冲突

// 动态导入 Electron，避免类型错误
let ipcMain: any = null;
let electronLoadError: string | null = null;

try {
  const electron = require('electron');
  ipcMain = electron.ipcMain;
} catch (error: any) {
  electronLoadError = error.message;
  console.error('[Plugin] Failed to load Electron:', error);
}

interface TerminalMessage {
  type: 'terminal-output' | 'terminal-exit' | 'terminal-created' | 'ready' | 'error' | 'pong';
  terminalId?: number;
  data?: string;
  code?: number;
  signal?: string;
  success?: boolean;
  error?: string;
  stack?: string;
}

class PTYTerminalManager {
  private ptyServerProcess: ChildProcess | null = null;
  private isDestroyed: boolean = false;
  private isInitialized: boolean = false;
  private isServerReady: boolean = false;
  private activeTerminals: Set<number> = new Set();
  private messageQueue: any[] = [];
  private messageHandlers: Set<(message: TerminalMessage) => void> = new Set();

  async initialize() {
    try {
      // 如果已经有进程在运行，先停止它
      if (this.ptyServerProcess) {
        await this.stopPTYServer();
      }
      
      // 启动 PTY 服务器子进程
      await this.startPTYServer();
      
      this.isInitialized = true;
    } catch (error: any) {
      console.error('[PTY] Failed to initialize PTY terminal manager:', error);
      throw error;
    }
  }

  // 检查服务器是否正在运行
  isRunning(): boolean {
    return this.ptyServerProcess !== null && this.isServerReady;
  }

  // 启动服务器（如果未启动）
  async startIfNotRunning() {
    if (!this.isRunning()) {
      // Starting PTY server on demand
      await this.initialize();
    } else {
              // Server already running, reusing existing instance
    }
  }

  // 停止服务器
  async stop() {
    if (this.isRunning()) {
      // Stopping PTY server
      await this.stopPTYServer();
    } else {
              // Server not running, nothing to stop
    }
  }

  // 重启 PTY 服务器
  async restart() {
    try {
      await this.initialize();
    } catch (error: any) {
      console.error('[PTY] Failed to restart PTY terminal manager:', error);
      throw error;
    }
  }

  // 停止 PTY 服务器
  private async stopPTYServer(): Promise<void> {
    return new Promise((resolve) => {
      if (!this.ptyServerProcess) {
        resolve();
        return;
      }

      // 清理状态
      this.isServerReady = false;
      this.activeTerminals.clear();
      this.messageQueue.length = 0;
      this.cleanupMessageHandlers();

      const process = this.ptyServerProcess;
      this.ptyServerProcess = null;

      // 设置退出监听器
      const onExit = () => {
        resolve();
      };

      process.once('exit', onExit);

      // 优雅关闭
      process.kill('SIGTERM');

      // 如果 3 秒后还没关闭，强制杀死
      setTimeout(() => {
        if (!process.killed) {
          process.kill('SIGKILL');
        }
        // 确保 resolve 被调用
        setTimeout(resolve, 500);
      }, 3000);
    });
  }

  private async startPTYServer(): Promise<void> {
    return new Promise((resolve, reject) => {
      // 如果已经有进程在运行，先停止它
      if (this.ptyServerProcess) {
        this.stopPTYServer().then(() => {
          this.startPTYServerInternal().then(resolve).catch(reject);
        }).catch(reject);
        return;
      }

      this.startPTYServerInternal().then(resolve).catch(reject);
    });
  }

  private async startPTYServerInternal(): Promise<void> {
    return new Promise((resolve, reject) => {
      // 使用系统 Node.js 启动独立的 PTY 服务器
      const ptyServerPath = path.join(__dirname, 'pty-server.cjs');
      
      // 确保工作目录在项目根目录，这样可以正确找到 node_modules
      const projectRoot = path.dirname(__dirname);
      
      // 使用系统 Node.js
      const nodePath = 'node';
      
      // 准备子进程环境变量 - 清理 Electron 相关变量
      const subprocessEnv = { ...process.env };
      const electronKeys = Object.keys(subprocessEnv).filter(key => 
        key.startsWith('ELECTRON_') || 
        key.startsWith('CHROME_') || 
        key.startsWith('GOOGLE_') ||
        key.includes('CHROMIUM')
      );
      
      electronKeys.forEach(key => delete subprocessEnv[key]);
      
      this.ptyServerProcess = spawn(nodePath, [ptyServerPath], {
        stdio: ['pipe', 'pipe', 'pipe', 'ipc'],
        env: subprocessEnv, // 使用配置后的环境变量
        cwd: projectRoot, // 设置工作目录为项目根目录
        detached: false
      });

      // 监听 PTY 服务器的标准输出（日志信息）
      this.ptyServerProcess.stdout?.on('data', (data) => {
        const output = data.toString().trim();
        if (output) {
          console.log(output); // 直接输出PTY服务器的日志
        }
      });

      // 监听 PTY 服务器的错误输出
      this.ptyServerProcess.stderr?.on('data', (data) => {
        const error = data.toString();
        console.error('[PTY Server Error]', error.trim());
      });

      // 监听来自 PTY 服务器的消息
      this.ptyServerProcess.on('message', (message: TerminalMessage) => {
        this.handlePTYServerMessage(message);
        
        // 服务器就绪信号
        if (message.type === 'ready' && !this.isServerReady) {
          this.isServerReady = true;
          
          // 处理排队的消息
          this.processMessageQueue();
          resolve();
        }
      });

      // 监听进程退出
      this.ptyServerProcess.on('exit', (code, signal) => {
        this.ptyServerProcess = null;
        this.isServerReady = false;
        
        // 如果不是主动销毁，尝试重启
        if (!this.isDestroyed && code !== 0) {
          setTimeout(() => {
            if (!this.isDestroyed) {
              this.startPTYServer().catch(error => {
                console.error('[PTY Server] Failed to restart:', error);
              });
            }
          }, 2000);
        }
      });

      // 监听进程错误
      this.ptyServerProcess.on('error', (error) => {
        console.error('[PTY Server] Process error:', error);
        reject(error);
      });

      // 超时处理 - 增加到 30 秒
      const timeout = setTimeout(() => {
        if (!this.isServerReady) {
          if (this.ptyServerProcess) {
            this.ptyServerProcess.kill('SIGKILL');
            this.ptyServerProcess = null;
          }
          reject(new Error('PTY server startup timeout'));
        }
      }, 30000);

      // 清理超时定时器
      const originalResolve = resolve;
      resolve = () => {
        clearTimeout(timeout);
        originalResolve();
      };
    });
  }

  private handlePTYServerMessage(message: TerminalMessage) {
    // 转发终端输出到渲染进程
    if (message.type === 'terminal-output' && message.terminalId) {
      // 通过 webContents 发送到渲染进程
      // 这里需要在 IPC 处理器中处理
    } else if (message.type === 'terminal-exit' && message.terminalId) {
      this.activeTerminals.delete(message.terminalId);
    } else if (message.type === 'error') {
      console.error('[PTY Server] Error:', message.error);
    }
  }

  private sendToPTYServer(message: any) {
    if (this.ptyServerProcess && this.isServerReady) {
      this.ptyServerProcess.send(message);
    } else {
      this.messageQueue.push(message);
    }
  }

  private processMessageQueue() {
    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift();
      if (this.ptyServerProcess) {
        this.ptyServerProcess.send(message);
      }
    }
  }

  // 创建新终端
  async createTerminal(options: any = {}): Promise<number> {
    return new Promise((resolve, reject) => {
      if (!this.isInitialized) {
        reject(new Error('PTY manager not initialized'));
        return;
      }

      const messageHandler = (message: TerminalMessage) => {
        if (message.type === 'terminal-created' && message.success && message.terminalId) {
          this.activeTerminals.add(message.terminalId);
          this.ptyServerProcess?.off('message', messageHandler);
          resolve(message.terminalId);
        }
      };

      this.ptyServerProcess?.on('message', messageHandler);
      
      this.sendToPTYServer({
        type: 'create-terminal',
        options
      });

      // 超时处理
      setTimeout(() => {
        this.ptyServerProcess?.off('message', messageHandler);
        reject(new Error('Create terminal timeout'));
      }, 5000);
    });
  }

  // 写入数据到终端
  writeToTerminal(terminalId: number, data: string) {
    this.sendToPTYServer({
      type: 'write-terminal',
      terminalId,
      data
    });
  }

  // 调整终端大小
  resizeTerminal(terminalId: number, cols: number, rows: number) {
    this.sendToPTYServer({
      type: 'resize-terminal',
      terminalId,
      data: { cols, rows }
    });
  }

  // 销毁终端
  destroyTerminal(terminalId: number) {
    console.log(`[PTY Manager] Requesting destroy of terminal ${terminalId}`);
    this.sendToPTYServer({
      type: 'destroy-terminal',
      terminalId
    });
    this.activeTerminals.delete(terminalId);
  }

  // 清理消息处理器
  private cleanupMessageHandlers() {
    if (this.ptyServerProcess && this.messageHandlers.size > 0) {
      for (const handler of this.messageHandlers) {
        this.ptyServerProcess.removeListener('message', handler);
      }
      this.messageHandlers.clear();
    }
  }

  setupIpcHandlers() {
    if (!ipcMain) {
      console.error('[PTY] ipcMain not available, cannot setup handlers');
      return;
    }

    // 创建终端
    ipcMain.handle('terminal-create', async (event: any, options: any) => {
      try {
        // 设置默认工作目录为项目目录（插件目录的上两级）
        const pluginDir = path.dirname(__dirname);  // 插件根目录
        const projectDir = path.dirname(path.dirname(pluginDir));  // 项目目录（上两级）
        
        const terminalOptions = {
          cols: 80,
          rows: 24,
          cwd: projectDir,  // 使用项目目录作为默认工作目录
          ...options // 允许前端选项覆盖默认值
        };
        

        
        if (!this.isInitialized) {
          await this.initialize();
        }
        const terminalId = await this.createTerminal(terminalOptions);
        return { success: true, terminalId };
      } catch (error: any) {
        console.error('[PTY] Failed to create terminal:', error);
        return { success: false, error: error.message };
      }
    });

    // 写入终端数据
    ipcMain.handle('terminal-write', async (event: any, terminalId: any, data: any) => {
      try {
        this.writeToTerminal(terminalId, data);
        return { success: true };
      } catch (error: any) {
        console.error('[PTY] Failed to write to terminal:', error);
        return { success: false, error: error.message };
      }
    });

    // 调整终端大小
    ipcMain.handle('terminal-resize', async (event: any, terminalId: any, cols: any, rows: any) => {
      try {
        this.resizeTerminal(terminalId, cols, rows);
        return { success: true };
      } catch (error: any) {
        console.error('[PTY] Failed to resize terminal:', error);
        return { success: false, error: error.message };
      }
    });

    // 销毁终端
    ipcMain.handle('terminal-destroy', async (event: any, terminalId: any) => {
      try {
        this.destroyTerminal(terminalId);
        return { success: true };
      } catch (error: any) {
        console.error('[PTY] Failed to destroy terminal:', error);
        return { success: false, error: error.message };
      }
    });

    // 检查状态
    ipcMain.handle('terminal-check-status', async () => {
      const isRunning = this.ptyServerProcess !== null && this.isServerReady;
      return { 
        isRunning, 
        isInitialized: this.isInitialized,
        activeTerminals: Array.from(this.activeTerminals)
      };
    });

    // 重启 PTY 服务器
    ipcMain.handle('terminal-restart', async () => {
      try {
        await this.restart();
        return { success: true };
      } catch (error: any) {
        console.error('[PTY] Failed to restart:', error);
        return { success: false, error: error.message };
      }
    });

    // 面板打开时启动服务
    ipcMain.handle('terminal-panel-opened', async () => {
      try {
        // Panel opened, starting server if needed
        await this.startIfNotRunning();
        return { success: true, message: 'Server started or already running' };
      } catch (error: any) {
        console.error('[PTY] Failed to start server on panel open:', error);
        return { success: false, error: error.message };
      }
    });

    // 面板关闭时停止服务
          ipcMain.handle('terminal-panel-closed', async () => {
        try {
          // Panel closed, stopping server
          await this.stop();
        return { success: true, message: 'Server stopped' };
      } catch (error: any) {
        console.error('[PTY] Failed to stop server on panel close:', error);
        return { success: false, error: error.message };
      }
    });

    // 获取项目路径
    ipcMain.handle('get-project-path', async () => {
      try {
        // 插件目录：/path/to/project/extensions/kylin-ai-terminal
        // 项目目录：/path/to/project (上两级)
        const pluginDir = path.dirname(__dirname); // 从 dist 目录往上一级到插件根目录
        const projectDir = path.dirname(path.dirname(pluginDir)); // 再往上两级到项目目录
        
        // 验证路径是否存在
        const fs = require('fs');
        if (!fs.existsSync(projectDir)) {
          throw new Error(`Project directory does not exist: ${projectDir}`);
        }
        
        return { 
          success: true, 
          projectPath: projectDir,
          pluginPath: pluginDir
        };
      } catch (error: any) {
        console.error('[PTY] Failed to get project path:', error);
        return { success: false, error: error.message };
      }
    });

    // 设置终端数据监听器
    ipcMain.handle('terminal-setup-listener', async (event: any) => {
      const webContents = event.sender;
      
      // 先清理旧的消息处理器
      this.cleanupMessageHandlers();
      
      // 创建消息转发器
      const messageHandler = (message: TerminalMessage) => {
        if (message.type === 'terminal-output' && message.terminalId) {
          webContents.send('terminal-data', {
            terminalId: message.terminalId,
            data: message.data
          });
        } else if (message.type === 'terminal-exit' && message.terminalId) {
          webContents.send('terminal-exit', {
            terminalId: message.terminalId,
            code: message.code,
            signal: message.signal
          });
        }
      };

      // 记录处理器并添加到进程
      this.messageHandlers.add(messageHandler);
      if (this.ptyServerProcess) {
        this.ptyServerProcess.on('message', messageHandler);
      }

      return { success: true };
    });


  }

  destroy() {
    this.isDestroyed = true;
    this.isInitialized = false;
    this.isServerReady = false;

    if (this.ptyServerProcess) {
      // 优雅关闭
      this.ptyServerProcess.kill('SIGTERM');
      
      // 如果 3 秒后还没关闭，强制杀死
      setTimeout(() => {
        if (this.ptyServerProcess && !this.ptyServerProcess.killed) {
          this.ptyServerProcess.kill('SIGKILL');
        }
      }, 3000);
      
      this.ptyServerProcess = null;
    }

    this.activeTerminals.clear();
  }
}

// 全局终端管理器实例
let terminalManager: PTYTerminalManager | null = null;

// 立即初始化终端管理器和 IPC 处理器
function initializeTerminalManager() {
  if (!terminalManager) {
    terminalManager = new PTYTerminalManager();
    // 只设置 IPC 处理器，不启动服务器
    terminalManager.setupIpcHandlers();
  }
}

// 模块加载时立即设置 IPC 处理器
initializeTerminalManager();

// 插件加载函数
export function load() {
  // Loading kylin-ai-terminal plugin
  
  // 确保终端管理器已初始化
  initializeTerminalManager();
}

// 插件卸载函数
export function unload() {
  // Unloading kylin-ai-terminal plugin
  
  if (terminalManager) {
    terminalManager.destroy();
    terminalManager = null;
  }
}

// 处理进程退出
process.on('SIGTERM', () => {
  if (terminalManager) {
    terminalManager.destroy();
  }
  process.exit(0);
});

process.on('SIGINT', () => {
  if (terminalManager) {
    terminalManager.destroy();
  }
  process.exit(0);
});

// 处理未捕获的异常
process.on('uncaughtException', (error) => {
  console.error('[Process] Uncaught exception:', error);
  if (terminalManager) {
    terminalManager.destroy();
  }
});

// 导出消息处理方法
export const methods = {
  async 'open-terminal-panel'() {
    // 打开面板
    if (typeof Editor !== 'undefined' && Editor.Panel) {
      await Editor.Panel.open('kylin-ai-terminal');
    }
    
    return { success: true };
  }
};
