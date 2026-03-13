<template>
  <div class="index-container">
    <div class="header">
      <h1>用户管理系统</h1>
      <div class="user-info">
        <span>欢迎，{{ currentUser.username }}</span>
        <button @click="logout" class="logout-button">退出登录</button>
      </div>
    </div>
    
    <div class="content">
      <div class="toolbar">
        <button @click="showAddDialog" class="action-button add-button">添加用户</button>
        <button @click="showMcpChat" class="action-button mcp-button">MCP对话</button>
      </div>
      
      <div class="table-container">
        <table class="user-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in userList" :key="user.id">
              <td>{{ user.id }}</td>
              <td>{{ user.username }}</td>
              <td>
                <button @click="showEditDialog(user)" class="action-button edit-button">修改</button>
                <button @click="deleteUser(user.id)" class="action-button delete-button">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MCP对话窗口 -->
    <div v-if="showMcpDialog" class="mcp-dialog-overlay" @click="closeMcpDialog">
      <div class="mcp-dialog" @click.stop>
        <div class="mcp-header">
          <h3>MCP对话</h3>
          <button @click="closeMcpDialog" class="close-button">×</button>
        </div>
        <div class="mcp-chat-window" ref="chatWindow">
          <div v-for="(message, index) in chatMessages" :key="index" :class="['message', message.type]">
            <div class="message-content">{{ message.content }}</div>
          </div>
        </div>
        <div class="mcp-input-area">
          <textarea 
            v-model="chatInput" 
            placeholder="请输入您的问题..." 
            @keydown.enter.prevent="sendMessage"
            :disabled="chatLoading"
          ></textarea>
          <button @click="sendMessage" class="send-button" :disabled="chatLoading || !chatInput.trim()">
            {{ chatLoading ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 添加/编辑用户对话框 -->
    <div v-if="showDialog" class="dialog-overlay" @click="closeDialog">
      <div class="dialog" @click.stop>
        <h3>{{ isEditMode ? '修改用户' : '添加用户' }}</h3>
        <div class="form-group">
          <label>用户名</label>
          <input type="text" v-model="formData.username" placeholder="请输入用户名">
        </div>
        <div class="form-group">
          <label>密码</label>
          <input type="password" v-model="formData.password" :placeholder="isEditMode ? '留空则不修改密码' : '请输入密码'">
        </div>
        <div class="dialog-actions">
          <button @click="closeDialog" class="action-button cancel-button">取消</button>
          <button @click="saveUser" class="action-button save-button" :disabled="loading">
            {{ loading ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'IndexPage',
  data() {
    return {
      currentUser: {},
      userList: [],
      showDialog: false,
      isEditMode: false,
      loading: false,
      pollingInterval: null, // 轮询定时器
      formData: {
        id: null,
        username: '',
        password: ''
      },
      // MCP对话相关
      showMcpDialog: false,
      chatMessages: [],
      chatInput: '',
      chatLoading: false
    }
  },
  mounted() {
    this.checkLogin();
    this.loadUserList();
    // 启动定时轮询，每3秒刷新一次用户列表
    this.pollingInterval = setInterval(() => {
      this.loadUserList();
    }, 3000);
    // 监听浏览器窗口焦点事件，当窗口获得焦点时刷新数据
    window.addEventListener('focus', this.loadUserList);
  },
  
  beforeUnmount() {
    // 清除定时器
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
    // 移除焦点事件监听器
    window.removeEventListener('focus', this.loadUserList);
  },
  methods: {
    checkLogin() {
      const user = localStorage.getItem('user');
      if (!user) {
        this.$router.push('/');
        return;
      }
      this.currentUser = JSON.parse(user);
    },
    
    async loadUserList() {
      try {
        const token = localStorage.getItem('token');
        const response = await axios.get('/user/list', {
          headers: {
            'Authorization': token
          }
        });
        
        if (response.data.code === 200) {
          this.userList = response.data.data;
        } else {
          console.error('加载用户列表失败：' + response.data.message);
          // 静默处理错误，避免频繁弹窗
        }
      } catch (error) {
        console.error('加载用户列表失败:', error);
        // 静默处理错误，避免频繁弹窗
      }
    },
    
    showAddDialog() {
      this.isEditMode = false;
      this.formData = {
        id: null,
        username: '',
        password: ''
      };
      this.showDialog = true;
    },
    
    showEditDialog(user) {
      this.isEditMode = true;
      this.formData = {
        id: user.id,
        username: user.username,
        password: ''
      };
      this.showDialog = true;
    },
    
    closeDialog() {
      this.showDialog = false;
      this.formData = {
        id: null,
        username: '',
        password: ''
      };
    },
    
    async saveUser() {
      if (!this.formData.username) {
        alert('请输入用户名');
        return;
      }
      
      if (!this.isEditMode && !this.formData.password) {
        alert('请输入密码');
        return;
      }
      
      this.loading = true;
      
      try {
        const token = localStorage.getItem('token');
        let response;
        
        if (this.isEditMode) {
          response = await axios.put('/user/update', {
            id: this.formData.id,
            username: this.formData.username,
            password: this.formData.password
          }, {
            headers: {
              'Authorization': token
            }
          });
        } else {
          response = await axios.post('/user/add', {
            username: this.formData.username,
            password: this.formData.password
          }, {
            headers: {
              'Authorization': token
            }
          });
        }
        
        if (response.data.code === 200) {
          alert(this.isEditMode ? '修改成功' : '添加成功');
          this.closeDialog();
          this.loadUserList();
        } else {
          alert('操作失败：' + response.data.message);
        }
      } catch (error) {
        console.error('操作失败:', error);
        alert('操作失败，请稍后重试');
      } finally {
        this.loading = false;
      }
    },
    
    async deleteUser(id) {
      if (!confirm('确定要删除该用户吗？')) {
        return;
      }
      
      try {
        const token = localStorage.getItem('token');
        const response = await axios.delete(`/user/${id}`, {
          headers: {
            'Authorization': token
          }
        });
        
        if (response.data.code === 200) {
          alert('删除成功');
          this.loadUserList();
        } else {
          alert('删除失败：' + response.data.message);
        }
      } catch (error) {
        console.error('删除失败:', error);
        alert('删除失败，请稍后重试');
      }
    },
    
    logout() {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      this.$router.push('/');
    },
    
    // MCP对话相关方法
    showMcpChat() {
      this.showMcpDialog = true;
      this.chatMessages = [];
      this.chatInput = '';
    },
    
    closeMcpDialog() {
      this.showMcpDialog = false;
      this.chatMessages = [];
      this.chatInput = '';
    },
    
    async sendMessage() {
      if (!this.chatInput.trim() || this.chatLoading) {
        return;
      }
      
      const userMessage = this.chatInput.trim();
      this.chatMessages.push({
        type: 'user',
        content: userMessage
      });
      this.chatInput = '';
      this.chatLoading = true;
      
      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom();
      });
      
      try {
        const token = localStorage.getItem('token');
        const response = await axios.post('/mcp/chat', {
          message: userMessage
        }, {
          headers: {
            'Authorization': token,
            'Content-Type': 'application/json'
          }
        });
        
        if (response.data.code === 200) {
          this.chatMessages.push({
            type: 'assistant',
            content: response.data.data || response.data.message
          });
        } else {
          this.chatMessages.push({
            type: 'assistant',
            content: '抱歉，服务暂时不可用：' + response.data.message
          });
        }
      } catch (error) {
        console.error('MCP对话失败:', error);
        this.chatMessages.push({
          type: 'assistant',
          content: '抱歉，请求失败，请稍后重试'
        });
      } finally {
        this.chatLoading = false;
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }
    },
    
    scrollToBottom() {
      const chatWindow = this.$refs.chatWindow;
      if (chatWindow) {
        chatWindow.scrollTop = chatWindow.scrollHeight;
      }
    }
  }
}
</script>

<style scoped>
.index-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  background-color: #4CAF50;
  color: white;
  padding: 20px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header h1 {
  margin: 0;
  font-size: 24px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logout-button {
  padding: 8px 16px;
  background-color: white;
  color: #4CAF50;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.logout-button:hover {
  background-color: #f0f0f0;
}

.content {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.toolbar {
  margin-bottom: 20px;
}

.table-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th,
.user-table td {
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.user-table th {
  background-color: #f8f8f8;
  font-weight: 600;
  color: #333;
}

.user-table tbody tr:hover {
  background-color: #f5f5f5;
}

.action-button {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  margin-right: 8px;
}

.add-button {
  background-color: #4CAF50;
  color: white;
}

.add-button:hover {
  background-color: #45a049;
}

.edit-button {
  background-color: #2196F3;
  color: white;
}

.edit-button:hover {
  background-color: #0b7dda;
}

.delete-button {
  background-color: #f44336;
  color: white;
}

.delete-button:hover {
  background-color: #da190b;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background-color: white;
  border-radius: 8px;
  padding: 30px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.dialog h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
}

.dialog .form-group {
  margin-bottom: 20px;
}

.dialog .form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.dialog .form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.cancel-button {
  background-color: #ccc;
  color: white;
}

.cancel-button:hover {
  background-color: #b3b3b3;
}

.save-button {
  background-color: #4CAF50;
  color: white;
}

.save-button:hover {
  background-color: #45a049;
}

.action-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

/* MCP对话窗口样式 */
.mcp-button {
  background-color: #9C27B0;
  color: white;
  margin-left: 10px;
}

.mcp-button:hover {
  background-color: #7B1FA2;
}

.mcp-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.mcp-dialog {
  background-color: white;
  border-radius: 8px;
  width: 600px;
  max-width: 90%;
  height: 500px;
  max-height: 90%;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.mcp-header {
  background-color: #9C27B0;
  color: white;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 8px 8px 0 0;
}

.mcp-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-button {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  background-color: rgba(255,255,255,0.2);
  border-radius: 4px;
}

.mcp-chat-window {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9f9f9;
}

.message {
  margin-bottom: 15px;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 10px 15px;
  border-radius: 18px;
  word-wrap: break-word;
}

.message.user .message-content {
  background-color: #9C27B0;
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-content {
  background-color: white;
  color: #333;
  border: 1px solid #e0e0e0;
  border-bottom-left-radius: 4px;
}

.mcp-input-area {
  padding: 15px 20px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  gap: 10px;
  background-color: white;
  border-radius: 0 0 8px 8px;
}

.mcp-input-area textarea {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
  height: 60px;
  font-family: inherit;
  font-size: 14px;
}

.mcp-input-area textarea:focus {
  outline: none;
  border-color: #9C27B0;
}

.send-button {
  padding: 10px 20px;
  background-color: #9C27B0;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  align-self: flex-end;
}

.send-button:hover {
  background-color: #7B1FA2;
}

.send-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}
</style>