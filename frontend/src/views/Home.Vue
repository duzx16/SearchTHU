<template>
  <div>
    <header-navbar></header-navbar>
    <b-container fluid class="container-main">
      <a class="logo" href=".">THU Search</a>
      <search-input class="input" v-on:newSearch="newSearch"></search-input>
    </b-container>
  </div>
</template>

<script>
import HeaderNavbar from '@/components/HeaderNavbar.vue'
import SearchInput from '@/components/SearchInput.vue'
import axios from 'axios'

export default {
  name: 'search',
  components: { HeaderNavbar, SearchInput },
  data () {
    return {
    }
  },
  methods: {
    newSearch (query) {
      window.location.assign(`/#/search/${query}/1`)
    },
    updateCompletion (query) {
      this.queryBuffer = query
      axios.get(
        '/api/_completion', {
          params: { 'query': query }
      })
      .then((response) => {
        this.candidates = response.data.queries
      })
    },
    autoCompletion () {
      if (this.query === this.queryBuffer) {
        return
      }
      this.updateCompletion(this.query)
    },
    test () {
      alert('hi')
    }
  },
  mounted () {
    // this.getResults()
  }
}
</script>

<style lang="scss" scoped>
.logo {
  display: block;
  text-align: center;
  font-family: Impact;
  font-size: 100px;
  vertical-align: middle;
  color: #6b008d;
  margin-top: 100px;
}

.logo:hover {
  text-decoration: none;
  color: #6b008d;
}

.input {
  text-align: center;
}
</style>
