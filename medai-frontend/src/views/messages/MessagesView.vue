<template>
  <div class="messages-view">
    <ChatWindow />
  </div>
</template>

<script>
import ChatWindow from './ChatWindow.vue'

export default {
  name: 'MessagesView',
  components: { ChatWindow }
}
</script>

<style scoped>
.messages-view { height: 100%; }
</style>