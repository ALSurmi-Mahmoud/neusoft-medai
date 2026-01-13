// /medai-frontend/vue.config.js
module.exports = {
  lintOnSave: false,

  devServer: {
    port: 8081,
    proxy: {
      "/api": {
        target: "http://localhost:8080", // <-- CHANGE to your backend port
        changeOrigin: true,
      },
    },
  },
};
