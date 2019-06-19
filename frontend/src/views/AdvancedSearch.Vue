<template>
  <div>
    <header-navbar></header-navbar>
    <b-container fluid class="container-main">
      <div class="head">
        <a class="logo" href=".">THU Search</a>
        <span class="advanced">Advanced Search</span>
      </div>
      <legend class="hint-advanced">Search with advanced conditions...</legend>
      <b-form class="form-advanced">
        <b-form-group label="This exact word or phase:" label-for="input-any" >
          <b-form-input id="input-exact" v-model="exact" type="text">
          </b-form-input>
        </b-form-group>        
        <b-form-group label="Any of these words:" label-for="input-any" >
          <b-form-input id="input-any" v-model="any" type="text">
          </b-form-input>
        </b-form-group>
        <b-form-group label="None of these words:" label-for="input-none" >
          <b-form-input id="input-none" v-model="none" type="text">
          </b-form-input>
        </b-form-group>  
        <b-form-group label="Site:" label-for="input-site" >
          <b-form-input id="input-site" v-model="none" type="text">
          </b-form-input>
        </b-form-group>              
        <b-form-group label="Terms appearing:" label-for="input-position" >
          <b-form-select v-model="position" :options="optionsPosition"></b-form-select>
        </b-form-group>        
        <b-form-group label="File type:" label-for="input-type" >
          <b-form-select v-model="type" :options="optionsType"></b-form-select>
        </b-form-group>  
        <div class="btn-box">
          <b-button class="btn-run" variant="primary" type="submit">
            Search
          </b-button> 
        </div>                
      </b-form>
    </b-container>
  </div>
</template>

<script>
import HeaderNavbar from '@/components/HeaderNavbar.vue'
import axios from 'axios'

export default {
  name: 'advanced-search',
  components: { HeaderNavbar },
  data () {
    const optionsPosition = [
      'anywhere in the page',
      'in the title of the page',
      'in the content of the page',
      'in the URL of the page'
    ]
    const optionsType = [
      'any format',
      'general web page (.html)',
      'Adobe Acrobat PDF (.pdf)',
      'Microsoft Word (.doc/.docx)'
    ]
    return {
      exact: '',
      any: '',
      none: '',
      site: '', // TODO: check format
      optionsPosition,
      optionsType,
      position: optionsPosition[0],
      type: optionsType[0],
    }
  },
  methods: {
  }
}
</script>

<style lang="scss">
.head {
  .advanced {
    vertical-align: middle;
    font-weight: bold;
    font-size: 20px;
    color: #6b008d;
    margin-left: 10px;
  }
}

.hint-advanced {
  font-size: 18px;
  margin-top: 10px;
}

.form-advanced {
  width: 800px;
  margin-top: 10px;

  .btn-box {
    text-align: right;
    margin-top: 25px;
  }
}
</style>
