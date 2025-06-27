#!/usr/bin/env node

/**
 * PTY Server - 独立的终端服务器
 * 使用系统 Node.js 运行，避免 MODULE_VERSION 冲突
 * 通过 IPC 与主进程通信
 */

const path = require('path');
const os = require('os');

// PTY服务器启动

// 清理所有可能干扰的 Electron/Chromium 环境变量
const originalEnv = { ...process.env };
const electronVars = Object.keys(process.env).filter(key => 
  key.startsWith('ELECTRON_') || 
  key.startsWith('CHROME_') || 
  key.startsWith('GOOGLE_') ||
  key.includes('CHROMIUM')
);

electronVars.forEach(key => delete process.env[key]);

// 额外清理可能导致问题的变量
delete process.env.DISPLAY;
delete process.env.XDG_SESSION_TYPE;

// 检查 node_modules 路径
const fs = require('fs');

let pty;
try {
  pty = require('node-pty');
} catch (error) {
  console.error('[PTY Server] Failed to load node-pty:', error.message);
  console.error('[PTY Server] Error code:', error.code);
  
  // 恢复环境变量
  process.env = originalEnv;
  throw error;
}

// 恢复环境变量
process.env = originalEnv;

// 存储终端会话
const terminals = new Map();
let terminalCounter = 0;

// 根据平台选择默认 shell
function getDefaultShell() {
  if (process.platform === 'win32') {
    return process.env.COMSPEC || 'cmd.exe';
  } else {
    return process.env.SHELL || '/bin/bash';
  }
}

// 创建新的终端会话
function createTerminal(options = {}) {
  const terminalId = ++terminalCounter;
  
  const defaultOptions = {
    name: 'xterm-256color',
    cols: options.cols || 80,
    rows: options.rows || 24,
    cwd: options.cwd || process.env.HOME || os.homedir(),
    env: { 
      ...process.env, 
      ...options.env,
      TERM: 'xterm-256color',
      // 确保交互式程序能正确检测终端能力
      COLORTERM: 'truecolor',
      // 设置合适的语言环境
      LC_ALL: process.env.LC_ALL || 'en_US.UTF-8',
      LANG: process.env.LANG || 'en_US.UTF-8'
    },
    // 关键：确保 PTY 使用原始模式，正确传递所有控制序列
    handleFlowControl: false,
    windowsHide: false
  };

  const shell = options.shell || getDefaultShell();

  try {
    const ptyProcess = pty.spawn(shell, options.args || [], defaultOptions);
    
    terminals.set(terminalId, ptyProcess);
    console.log(`[PTY Server] Created terminal ${terminalId} with shell: ${shell}`);

    // 监听终端输出
    ptyProcess.on('data', (data) => {
      if (process.send) {
        // 确保数据以 Buffer 或字符串形式正确传输，保持 ANSI 转义序列
        const dataToSend = typeof data === 'string' ? data : data.toString('utf8');
        process.send({
          type: 'terminal-output',
          terminalId,
          data: dataToSend
        });
      }
    });

    // 监听终端退出
    ptyProcess.on('exit', (code, signal) => {
      terminals.delete(terminalId);
      console.log(`[PTY Server] Terminal ${terminalId} exited with code: ${code}, signal: ${signal}`);
      if (process.send) {
        process.send({
          type: 'terminal-exit',
          terminalId,
          code,
          signal
        });
      }
    });

    return terminalId;
  } catch (error) {
    console.error(`[PTY Server] Failed to create terminal ${terminalId}:`, error);
    throw error;
  }
}

// 写入数据到终端
function writeToTerminal(terminalId, data) {
  const terminal = terminals.get(terminalId);
  if (terminal) {
    terminal.write(data);
  } else {
    console.warn(`[PTY Server] Terminal ${terminalId} not found`);
  }
}

// 调整终端大小
function resizeTerminal(terminalId, cols, rows) {
  const terminal = terminals.get(terminalId);
  if (terminal) {
    terminal.resize(cols, rows);
  } else {
    console.warn(`[PTY Server] Terminal ${terminalId} not found for resize`);
  }
}

// 销毁终端
function destroyTerminal(terminalId) {
  const terminal = terminals.get(terminalId);
  if (terminal) {
    console.log(`[PTY Server] Destroying terminal ${terminalId}`);
    terminal.kill();
    terminals.delete(terminalId);
  } else {
    console.warn(`[PTY Server] Terminal ${terminalId} not found for destroy`);
  }
}

// 处理来自主进程的消息
process.on('message', (message) => {
  try {
    const { type, terminalId, data, options } = message;

    switch (type) {
      case 'create-terminal':
        const newTerminalId = createTerminal(options);
        process.send({
          type: 'terminal-created',
          terminalId: newTerminalId,
          success: true
        });
        break;

      case 'write-terminal':
        writeToTerminal(terminalId, data);
        break;

      case 'resize-terminal':
        resizeTerminal(terminalId, data.cols, data.rows);
        break;

      case 'destroy-terminal':
        destroyTerminal(terminalId);
        break;

      case 'ping':
        process.send({ type: 'pong' });
        break;

      default:
        console.warn('[PTY Server] Unknown message type:', type);
    }
  } catch (error) {
    console.error('[PTY Server] Error handling message:', error);
    process.send({
      type: 'error',
      error: error.message,
      stack: error.stack
    });
  }
});

// 处理进程退出
process.on('SIGTERM', () => {
  terminals.forEach((terminal, id) => {
    terminal.kill();
  });
  process.exit(0);
});

process.on('SIGINT', () => {
  terminals.forEach((terminal, id) => {
    terminal.kill();
  });
  process.exit(0);
});

// 处理未捕获的异常
process.on('uncaughtException', (error) => {
  console.error('[PTY Server] Uncaught exception:', error);
  process.send({
    type: 'error',
    error: error.message,
    stack: error.stack
  });
});

// 立即发送就绪信号
if (process.send) {
  process.send({ type: 'ready' });
} 