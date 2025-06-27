<script setup lang="ts">
import { inject } from 'vue';

import { name } from '../../package.json';

import MultiTabTerminal from './components/MultiTabTerminal.vue';
import { state } from './pina';
import { keyAppRoot, keyMessage } from './provide-inject';

const appRootDom = inject(keyAppRoot);
const message = inject(keyMessage)!;

// 移除了可能导致UI冲突的ElMessage相关代码
// const open = () => {
//     ElMessage({
//         message: 'show message',
//         appendTo: appRootDom,
//     });
// };

function open2() {
    message({ message: 'show inject message' });
}

async function showVersion() {
    const version = await Editor.Message.request(name, 'get-version');
    message({ message: version });
}
</script>

<template>
    <div class="app-container">
        <MultiTabTerminal />
    </div>
</template>

<style scoped>
.app-container {
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
}
</style>
