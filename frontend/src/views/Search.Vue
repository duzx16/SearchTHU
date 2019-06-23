<template>
  <div>
    <header-navbar></header-navbar>
    <b-container fluid class="container-main">
      <div class="head">
        <a class="logo" href=".">THU Search</a>
        <search-input class="search-input" 
          v-on:newSearch="newSearch" v-on:goAdvanced="goAdvanced" ref="input">
        </search-input>
      </div>
      <div class="search-output">
        <div class="conditions" v-if="conditions !== ''">
          Advanced search conditions: {{ conditions }}
        </div>        
        <div class="total" v-if="total > 0">About {{ total.toLocaleString() }} results</div>
        <div class="no-result" v-if="total === 0 && !init">No matched document.</div>
        <div class="correct" v-if="correct !== ''">
          Showing results for <span>{{ correct }}</span>. 
          Or search for 
          <a v-on:click="searchWithoutCheck" href="javascript:;">
            {{ query }}
          </a> 
          instead
        </div>
        <div class="suggestion" v-if="suggestion !== ''">
          Did you mean:
          <a v-on:click="takeSuggestion" href="javascript:;">
            {{ suggestion }}
          </a> 
        </div>        
        <div class="document" v-for="item in documents" :key="item.index">
          <a class="title" v-html="item.title" v-bind:href="item.url" target="_blank"></a>
          <div class="content" v-html="item.content"></div>
          <span class="url">{{ item.url }}</span>
        </div>
        <div class="pagination" v-if="total > 0">
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
import SearchInput from '@/components/SearchInput.vue'
import axios from 'axios'

export default {
  name: 'search',
  components: { HeaderNavbar, SearchInput },
  data () {
    let query = ''
    if (this.$route.params.query !== undefined) {
      query = this.$route.params.query
    }
    let page = 1
    if (this.$route.params.page !== undefined) {
      page = parseInt(this.$route.params.page)
    }
    let advanced = {
      exact: this.$route.query.exact,
      any: this.$route.query.any,
      none: this.$route.query.none,
      site: this.$route.query.site,
      position: this.$route.query.position,
      type: this.$route.query.type
    }
    let advancedPath = '?', conditions = ''
    if (query.indexOf('(Advanced Search)') === 0) {
      advancedPath += '?'
      if (advanced.exact !== undefined && advanced.exact !== '') {
        conditions += ` exact: "${advanced.exact}";`
        advancedPath += `&exact=${advanced.exact}`
      }
      if (advanced.any !== undefined && advanced.any !== '') {
        conditions += ` any: "${advanced.any}";`
        advancedPath += `&any=${advanced.any}`
      }
      if (advanced.none !== undefined && advanced.none !== '') {
        conditions += ` none: "${advanced.none}";`
        advancedPath += `&none=${advanced.none}`
      }
      if (advanced.site !== undefined && advanced.site !== '') {
        conditions += ` site: "${advanced.site}";`
        advancedPath += `&site=${advanced.site}`
      }
      if (advanced.position !== undefined && advanced.position.indexOf('any') === -1) {
        conditions += ` position: "${advanced.position}";`
        advancedPath += `&position=${advanced.position}`
      }
      if (advanced.type !== undefined && advanced.type.indexOf('any') === -1) {
        conditions += ` type: "${advanced.type}";`
        advancedPath += `&type=${advanced.type}`
      }
    }
    let check = true
    if (this.$route.query.nocheck !== undefined) {
      check = false
    }
    history.pushState(
      {
        query,
        page,
        advanced
      },
      '', `/#/search/${query}/${page}${advancedPath}`
    )      
    return {
      query,
      page,
      advanced,
      advancedPath,
      conditions,
      check,      
      documents: [],
      total: 0,
      candidates: [],
      init: true,
      correct: '',
      suggestion: '',
    }
  },
  methods: {
    async getResults () {
      let response
      this.$refs.input.updateQuery(this.query)
      if (this.query.indexOf('(Advanced Search)') === 0) {
        let params = { 'page': this.page }        
        if (this.advanced.exact !== '') {
          params['exact'] = this.advanced.exact
        }
        if (this.advanced.any !== '') {
          params['any'] = this.advanced.any
        }
        if (this.advanced.none !== '') {
          params['none'] = this.advanced.none
        }
        if (this.advanced.site !== '') {
          params['site'] = this.advanced.site
        }                
        const positionOptions = ['any', 'title', 'content', 'link']
        for (let i = 0; i < positionOptions.length; ++i) {
          if (this.advanced.position.indexOf(positionOptions[i]) !== -1) {
            params['position'] = positionOptions[i]
          }
        }
        const typeOptions = ['any', 'html', 'pdf', 'doc/docx']
        for (let i = 0; i < typeOptions.length; ++i) {
          if (this.advanced.type.indexOf(typeOptions[i]) !== -1) {
            params['file_type'] = typeOptions[i]
          }
        }
        response = await axios.get('/api/_advanced_query', {
          params
        })
        this.correct = ''
      }
      else {
        response = await axios.get(
          '/api/_query', {
            params: { 'query': this.query, 'page': this.page }
        })
        if (this.check && response.data.documents.length === 0  && 
            response.data.correct !== undefined) {
          this.correct = response.data.correct
          response = await axios.get(
            '/api/_query', {
              params: { 'query': this.correct, 'page': this.page }
          })
        }
        else {
          this.correct = ''
          this.suggestion = ''
          if (response.data.correct !== undefined && 
              response.data.correct !== this.query) {
            this.suggestion = response.data.correct
          }
        }
      }
      console.log(response)
      if (response.data === null) {
        response.data = {
          documents: [],
          total: 0
        }
      }
      this.documents = response.data.documents
      this.total = response.data.total
      for (let i = 0; i < this.documents.length; ++i) {
        this.documents[i].index = i
        this.documents[i].url = 'http://' + this.documents[i].url
        if (this.documents[i].title === '' && this.documents[i].content.length > 0) {
          let contentWords = this.documents[i].content.split(' ')
          for (let j = 0; j < contentWords.length; ++j) {
            if (contentWords[j] !== '' && contentWords[j].length > 1) {
              this.documents[i].title = contentWords[j]
              break
            }
          }
        }
      }
      this.init = false
      window.scrollTo(0, 0)    
    },
    newSearch (query) {
      if (query === '') {
        window.location.assign('/#/')
        return
      }
      else if (query.indexOf('(Advanced Search)') === 0) {
        this.goAdvanced()
        return
      }
      else {
        window.location.assign(`/#/search/${query}/1`)
      }
    },
    searchWithoutCheck () {
      window.location.assign(`/#/search/${this.query}/1${this.advancedPath}?nocheck`)
    },
    takeSuggestion () {
      window.location.assign(`/#/search/${this.suggestion}/1${this.advancedPath}`)
    },
    goAdvanced () {
      var path = '/#/advanced_search?'
      if (this.advanced.exact !== undefined) {
        path += `&exact=${this.advanced.exact}`
      }
      if (this.advanced.any !== undefined) {
        path += `&any=${this.advanced.any}`
      }
      if (this.advanced.none !== undefined) {
        path += `&none=${this.advanced.none}`
      }
      if (this.advanced.site !== undefined) {
        path += `&site=${this.advanced.site}`
      }
      if (this.advanced.position !== undefined) {
        path += `&position=${this.advanced.position}`
      }
      if (this.advanced.type !== undefined) {
        path += `&type=${this.advanced.type}`
      }
      window.location.assign(path)
    },
    changePage (page) {
      window.location.assign(`/#/search/${this.query}/${page}${this.advancedPath}`)
    }
  },
  mounted () {
    this.getResults()
  },
  beforeRouteUpdate (to, from, next) {
    this.query = to.params.query
    this.page = parseInt(to.params.page)
    this.advanced = to.query
    this.getResults()
    next()
  }
}
</script>

<style lang="scss">
.search-output {
  width: 800px;

  .total, .conditions {
    font-size: 14px;
    margin-top: 10px;
    color: #777;
    max-width: 800px;
  }

  .no-result {
    margin-top: 10px;
  }

  .correct {
    margin-top: 5px;

    span, a {
      font-weight: bold;
    }
  }

  .suggestion {
    a {
      font-weight: bold;
    }
  }

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
      max-width: 800px;
      overflow-wrap: break-word;

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
