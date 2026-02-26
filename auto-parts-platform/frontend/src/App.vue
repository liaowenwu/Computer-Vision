<template>
  <el-container>
    <el-header>汽车配件爬虫后台管理平台</el-header>
    <el-main>
      <el-menu mode="horizontal" router>
        <el-menu-item index="/products">商品</el-menu-item>
        <el-menu-item index="/suppliers">供应商</el-menu-item>
        <el-menu-item index="/sync">同步任务</el-menu-item>
      </el-menu>
      <router-view />
    </el-main>
  </el-container>
</template>
