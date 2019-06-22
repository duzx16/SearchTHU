<template>
    <div class="search-input">
        <b-form @submit.prevent="goSearch">
            <b-form-input v-model="query" placeholder="" 
            v-on:input="autoCompletion" list="auto-completion-list">
            </b-form-input>
            <datalist id="auto-completion-list" autocomplete="off">
            <option v-for="candidate in candidates" v-bind:key="candidate">
                {{ candidate }}
            </option>
            </datalist>
            <b-button variant="primary" type="submit">
                Search
            </b-button>
            <b-button variant="primary" v-on:click="goAdvanced">
                Advanced
            </b-button>            
        </b-form>
    </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'search-input',
  data () {
    return {
      query: '',
      queryBuffer: '',
      candidates: []
    }
  },
  methods: {
    goSearch () {
      this.$emit('newSearch', this.query)
    },
    goAdvanced () {
      this.$emit('goAdvanced')
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
    updateQuery (query) {
      this.query = query
      this.updateCompletion(query)
    }
  }
}
</script>

<style lang="scss" scoped>
.search-input {
  form {
    input {
      display: inline-block;
      width: 500px;
      vertical-align: middle;
    }    
  }
}
</style>
