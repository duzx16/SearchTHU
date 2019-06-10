<template>
  <div>
    <header-navbar></header-navbar>
    <b-container fluid class="container-main">
      <div class="search-input">
        <b-form @submit="goSearch">
          <span class="title">THU Search</span>
          <b-form-input v-model="query" placeholder=""></b-form-input>
          <b-button class="btn-run" type="submit" variant="primary">
            Search
          </b-button>
        </b-form>
      </div>
      <div class="search-output">
        <div v-for="item in documents" :key="item.index">
          <a class="title" v-html="item.title" href="item.url"></a>
          <div class="content" v-html="item.content"></div>
        </div>
      </div>
    </b-container>
  </div>
</template>

<script>
import HeaderNavbar from '@/components/HeaderNavbar.vue'
import axios from 'axios'

export default {
  name: 'search',
  components: { HeaderNavbar },
  data () {
    let query = '清华'
    if (this.$route.params.query !== undefined) {
      query = this.$route.params.query
    }
    let page = 1
    if (this.$route.params.page !== undefined) {
      page = parseInt(this.$route.params.query)
    }
    return {
      query,
      page,
      documents: [],
      total: 0
    }
  },
  methods: {
    async getResults () {
      const response = await axios.get(
        '/api/_query', {
          params: { 'query': this.query, 'page': this.page }
      })
      console.log(response)
      this.documents = response.data.documents
      this.total = response.data.total
      for (let i = 0; i < this.documents.length; ++i) {
        this.documents[i].index = i
      }
    },
    goSearch () {
      this.page = 1
      this.getResults()
    }
  },
  mounted () {
    this.getResults()
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
