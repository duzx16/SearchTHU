<template>
  <header class="header-nav">
    <!-- <b-navbar toggleable="md" type="dark" variant="info">
      <b-navbar-toggle target="nav_collapse"></b-navbar-toggle>
      <b-navbar-brand href="/">{{ appName }}</b-navbar-brand>
    </b-navbar> -->
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
      appName: 'THU Search'
    }
  }
}
</script>

<style lang="scss">
.head {
  .logo {
    font-family: Impact;
    font-size: 24px;
    vertical-align: middle;
    color: #6b008d;
    margin-right: 10px;
  }

  .search-input {
    display: inline-block;
  }

  .logo:hover {
    text-decoration: none;
  }
}

.header-nav {
  position: fixed;
  top: 0;
  z-index: 100;
  width: 100%;
  overflow: hidden;
}

.container-main {
  padding: 20px 25px;
  margin-bottom: 20px;
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
  background-color: #7a00ab !important;
  border-color: #7a00ab !important;
}

a {
  color: #0075f2;
}

body {
  min-width: 780px;
}
</style>
