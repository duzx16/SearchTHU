<template>
  <div>
    <header-navbar></header-navbar>
    <b-container fluid class="container-main">
      <div class="search-input">
        <b-form @submit.prevent="goSearch">
          <a class="title" href=".">THU Search</a>
          <b-form-input v-model="query" placeholder="" 
            v-on:input="autoCompletion" list="auto-completion-list">
          </b-form-input>
          <datalist id="auto-completion-list" autocomplete="off">
            <option v-for="candidate in candidates" v-bind:key="candidate">
              {{ candidate }}
            </option>
          </datalist>
          <b-button class="btn-run" variant="primary" type="submit">
            Search
          </b-button>
        </b-form>
      </div>
      <div class="search-output">
        <div class="document" v-for="item in documents" :key="item.index">
          <a class="title" v-html="item.title" v-bind:href="item.url" target="_blank"></a>
          <div class="content" v-html="item.content"></div>
          <span class="url">{{ item.url }}</span>
        </div>
        <div class="pagination">
          <b-pagination size="md" :total-rows="total"
            v-model="page" v-on:change="changePage" :per-page="10" v-bind:limit=10
            hide-ellipsis hide-goto-end-buttons>
          </b-pagination>
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
      page = parseInt(this.$route.params.page)
    }
    history.pushState(
      {
        query,
        page
      },
      '', `/#/${query}/${page}`
    )   
    return {
      query,
      queryBuffer: query,
      page,
      documents: [],
      total: Math.max(10 * page, 0),
      candidates: []
    }
  },
  methods: {
    async getResults () {
      if (history.state !== null) {
        if (history.state.page !== undefined) {
          this.page = history.state.page
        }
        if (history.state.query !== undefined) {
          this.query = history.state.query
          this.updateCompletion(this.query)
        }
      }
      history.replaceState(
        {
          page: this.page,
          query: this.query
        },
        '', `/#/${this.query}/${this.page}`
      )
      const response = await axios.get(
        '/api/_query', {
          params: { 'query': this.query, 'page': this.page }
      })
      console.log(response)
      this.documents = response.data.documents
      this.total = response.data.total
      for (let i = 0; i < this.documents.length; ++i) {
        this.documents[i].index = i
        this.documents[i].url = 'http://' + this.documents[i].url
      }
    },
    goSearch () {
      this.page = 1
      history.pushState(
        {
          page: this.page,
          query: this.query
        },
        '', `/#/${this.query}/${this.page}`
      )
      this.getResults()
    },
    changePage (page) {
      this.page = page
      history.pushState(
        {
          page: this.page,
          query: this.query
        },
        '', `/#/${this.query}/${this.page}`
      )
      this.getResults()
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

  .title:hover {
    text-decoration: none;
  }
}

.search-output {
  width: 800px;

  .document {
    margin-top: 20px;

    .title {
      font-size: 18px;

      .highlight {
        color: #c00;
      }      
    }

    .url {
      max-width: 100%;
      font-size: 12px;
    }

    .content {
      font-size: 14px;

      .highlight {
        font-weight: bold;
        color: #c00;
      }
    }
  }

  .pagination {
    margin-top: 25px;
    width: 100%;

    .b-pagination {
      width: auto;
      margin: 0 auto;
    }
  }
}

</style>
