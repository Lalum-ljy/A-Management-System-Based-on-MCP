<template>
  <div class="login-container">
    <div class="login-left">
      <h1>这是一个mcp测试项目</h1>
    </div>
    <div class="login-right">
      <h2>登录</h2>
      <form class="login-form">
        <div class="form-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" placeholder="请输入用户名">
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" placeholder="请输入密码">
        </div>
        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
        <button type="button" class="login-button" @click="login" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'LoginPage',
  data() {
    return {
      username: '',
      password: '',
      loading: false,
      errorMessage: ''
    }
  },
  methods: {
    async login() {
      if (!this.username || !this.password) {
        this.errorMessage = '请输入用户名和密码';
        return;
      }

      this.loading = true;
      this.errorMessage = '';

      try {
        const response = await axios.post('/user/login', {
          username: this.username,
          password: this.password
        });

        if (response.data.code === 200) {
          // 登录成功，存储token
          localStorage.setItem('token', response.data.data.token);
          localStorage.setItem('user', JSON.stringify(response.data.data.user));
          // 跳转到首页
          console.log('登录成功:', response.data);
          this.$router.push('/index');
        } else {
          this.errorMessage = response.data.message;
        }
      } catch (error) {
        console.error('登录失败:', error);
        this.errorMessage = '登录失败，请稍后重试';
      } finally {
        this.loading = false;
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  background-image: url('../img/background.jpg');
  background-size: cover;
  background-position: center;
}

.login-left {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  padding: 20px;
}

.login-left h1 {
  font-size: 36px;
  font-weight: bold;
  text-align: center;
}

.login-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.9);
  padding: 40px;
}

.login-right h2 {
  font-size: 24px;
  margin-bottom: 30px;
  color: #333;
}

.login-form {
  width: 100%;
  max-width: 400px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.login-button {
  width: 100%;
  padding: 12px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  margin-top: 10px;
}

.login-button:hover {
  background-color: #45a049;
}

.login-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.error-message {
  color: #ff4d4f;
  font-size: 14px;
  margin-bottom: 15px;
  text-align: center;
}

@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
  }
  
  .login-left,
  .login-right {
    flex: none;
    width: 100%;
    height: 50%;
  }
}
</style>