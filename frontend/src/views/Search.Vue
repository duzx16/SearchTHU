<template>
  <div>
    <header-navbar></header-navbar>
    <b-container fluid class="container-main">
      <b-row class="search-input">
        <b-form @submit="goSearch">
          <span class="title">THU Search</span>
          <b-form-input v-model="text" placeholder=""></b-form-input>
          <b-button class="btn-run" type="submit" variant="primary">
            Search
          </b-button>
        </b-form>
      </b-row>
      <b-row class="search-output">
      </b-row>
    </b-container>
  </div>
</template>

<script>
import HeaderNavbar from '@/components/HeaderNavbar.vue'

export default {
  name: 'search',
  components: { HeaderNavbar },
  data () {
    return {
      appName: 'Tomasulo Simulator'
    }
  },
  methods: {
    goSearch () {
      alert('TODO')
    }
  }
}
</script>

<style lang="scss">
.search-input {
  form {
    input {
      display: inline-block;
      width: 600px;
      vertical-align: middle;
    }    
  }

  .title {
    font-family: Impact;
    font-size: 24px;
    vertical-align: middle;
    color: #6b008d;
    margin-right: 10px;
  }
}

</style>
