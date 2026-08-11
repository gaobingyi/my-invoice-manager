<template>
  <el-container class="layout">
    <el-header class="header">发票管理系统</el-header>
    <el-main>
      <InvoiceList />
    </el-main>
  </el-container>
</template>

<script setup>
import InvoiceList from './views/InvoiceList.vue'
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { background: #f5f7fa; }
.layout { min-height: 100vh; }
.header {
  display: flex;
  align-items: center;
  background: #409eff;
  color: #fff;
  font-size: 20px;
  font-weight: 600;
}
</style>
