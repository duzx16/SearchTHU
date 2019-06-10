<template>
  <header class="header-nav">
    <b-navbar toggleable="md" type="dark" variant="info">
      <b-navbar-toggle target="nav_collapse"></b-navbar-toggle>
      <b-navbar-brand href="/">{{ appName }}</b-navbar-brand>
    </b-navbar>
  </header>
</template>

<script>
export default {
  name: 'header-navbar',
  props: {
    atTask: Boolean
  },
  data () {
    return {
      appName: 'Tomasulo Simulator'
    }
  }
}
</script>

<style lang="scss">
.header-nav {
  position: fixed;
  top: 0;
  z-index: 100;
  width: 100%;
  overflow: hidden;
}

.container-main {
  padding: 0px 20px;
  margin-top: 70px;
}

.navbar-dark .navbar-nav {
  .nav-link {
    color: rgba(255, 255, 255, 0.85) !important;
  }
  .nav-link:hover {
    color: rgba(255, 255, 255, 0.95) !important;
  }
}

.btn-primary {
  background-color: #00a6bb !important;
  border-color: #00a6bb !important;
}

a {
  color: #0075f2;
}

body {
  min-width: 780px;
}
</style>
